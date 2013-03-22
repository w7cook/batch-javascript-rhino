import org.mozilla.javascript.Token;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.partition.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JSToPartition<E> {
  private PartitionFactory<E> factory;
  private String root;
  private Map<String, DynamicCallInfo> batchFunctionsInfo;

  public JSToPartition(
      PartitionFactory<E> factory,
      String root,
      Map<String, DynamicCallInfo> batchFunctionsInfo) {
    this.factory = factory;
    this.root = root;
    this.batchFunctionsInfo = batchFunctionsInfo;
  }

  public E exprFrom(AstNode node) {
    if (node == null) {
      return factory.Skip();
    }
    // paper TODO dates, true, false, ?:
    switch (node.getType()) {
      case Token.BLOCK:
        return exprFromBlock(node); // may be Block or Scope
      case Token.CALL:
        return exprFromFunctionCall((FunctionCall)node);
      case Token.EXPR_RESULT:
      case Token.EXPR_VOID:
        return exprFromExpressionStatement((ExpressionStatement)node);
      case Token.NAME:
        return exprFromName((Name)node);
      // Binary operators
      // Note: AND and OR are not listed here, since they carry a different
      // paper TODO basic implementation of AND, OR, NOT
	    //   AVG, MIN, MAX, COUNT, // aggregation
	    //   ASC, DESC, // sorting
	    //   GROUP; // mapping and grouping
      // meaning in javascript
      case Token.ADD:
      case Token.SUB:
      case Token.MUL:
      case Token.DIV:
      case Token.MOD:
      case Token.NE:
      case Token.EQ:
      case Token.LT:
      case Token.GT:
      case Token.LE:
      case Token.GE:
        return exprFromInfixExpression((InfixExpression)node);
      //case Token.NOT: // TODO: this has slightly different meaning in javascript
      //  return exprFromUnaryExpression((UnaryExpression)node);
      case Token.STRING:
        return exprFromStringLiteral((StringLiteral)node);
      case Token.NUMBER:
        return exprFromNumberLiteral((NumberLiteral)node);
      case Token.ASSIGN:
        return exprFromAssignment((Assignment)node);
      case Token.FOR:
        if (node instanceof ForInLoop) {
          return exprFromForInLoop((ForInLoop)node);
        } else {
          return exprFromOther(node);
        }
      case Token.VAR:
        return exprFromVariableDeclaration(
          (VariableDeclaration)node,
          factory.Skip()
        );
      case Token.IF:
        return exprFromIfStatement((IfStatement)node);
      case Token.FUNCTION:
        return exprFromFunctionNode((FunctionNode)node);
      case Token.RETURN:
        return exprFromReturnStatement((ReturnStatement)node);
      case Token.EMPTY:
        return factory.Skip();
      case Token.BATCH_INLINE:
        throw new Error(
          "Inlined batch functions must be called: <batch "
          + ((BatchInline)node).getFunctionName().toSource() + ">"
        );
      default:
        System.err.println("INCOMPLETE: "+Token.typeToName(node.getType())+" "+node.getClass().getName());
        return JSUtil.noimpl();
    }
  }

  private List<E> mapExprFrom(List<AstNode> nodes) {
    List<E> exprs = new ArrayList<E>(nodes.size());
    for (AstNode node : nodes) {
      exprs.add(exprFrom(node));
    }
    return exprs;
  }

  private E convertSequence(Iterator<Node> nodes) {
    LinkedList<E> sequence = new LinkedList<E>();
    while (nodes.hasNext()) {
      AstNode node = (AstNode)nodes.next();
      switch (node.getType()) {
        case Token.VAR:
          sequence.addLast(
            exprFromVariableDeclaration(
              (VariableDeclaration)node,
              convertSequence(nodes) // Note: surrounding loop will end since
                                     // recursive call will finish going
                                     // through the iterator
            )
          );
          break;
        default:
          sequence.addLast(exprFrom(node));
      }
    }
    return factory.Prim(Op.SEQ, sequence);
  }

  private E exprFromBlock(AstNode block) {
    return convertSequence(block.iterator());
  }

  private E exprFromExpressionStatement(ExpressionStatement statement) {
    return exprFrom(statement.getExpression());
  }

  private E exprFromFunctionCall(FunctionCall call) {
    AstNode target = call.getTarget();
    switch (target.getType()) {
      case Token.GETPROP:
        PropertyGet propGet = (PropertyGet)target;
        return factory.Call(
          exprFrom(propGet.getTarget()),
          propGet.getProperty().getIdentifier(),
          mapExprFrom(call.getArguments())
        );
      case Token.BATCH_INLINE:
        String funcName = JSUtil.identifierOf(
          ((BatchInline)target).getFunctionName()
        );
        if (funcName != null && batchFunctionsInfo.containsKey(funcName)) {
          return factory.setExtra(
            factory.DynamicCall(
              factory.Var("$$global$$"), // TODO: factory.Other(null)
              funcName,
              mapExprFrom(call.getArguments())
            ),
            batchFunctionsInfo.get(funcName)
          );
        } else {
          if (funcName != null) {
            throw new Error(
              "Batch call to <" + funcName + "> does not correspond to "
              + "a batch function of that name"
            );
          } else {
            // Should not occur on ast coming from the parser
            throw new Error(
              "batch keyword used incorrectly"
            );
          }
        }
      default:
        return JSUtil.noimpl();
    }
  }

  private E exprFromName(Name nameNode) {
    String name = nameNode.getIdentifier();
    if (root != null && name.equals(root)) { // TODO: inner scopes
      return factory.Root();
    } else if (name.equals(factory.RootName())) {
      return JSUtil.noimpl(); // TODO: avoid collisions
    } else {
      return factory.Var(name);
    }
  }

  private E exprFromInfixExpression(InfixExpression infix) {
    Op binOp;
    switch (infix.getOperator()) {
      case Token.ADD: binOp = Op.ADD; break;
      case Token.SUB: binOp = Op.SUB; break;
      case Token.MUL: binOp = Op.MUL; break;
      case Token.DIV: binOp = Op.DIV; break;
      case Token.MOD: binOp = Op.MOD; break;
      case Token.NE:  binOp = Op.NE;  break;
      case Token.EQ:  binOp = Op.EQ;  break;
      case Token.LT:  binOp = Op.LT;  break;
      case Token.GT:  binOp = Op.GT;  break;
      case Token.LE:  binOp = Op.LE;  break;
      case Token.GE:  binOp = Op.GE;  break;
      default:
        return JSUtil.noimpl();
    }
    return factory.Prim(
      binOp,
      exprFrom(infix.getLeft()),
      exprFrom(infix.getRight())
    );
  }

  private E exprFromStringLiteral(StringLiteral literal) {
    return factory.Data(literal.getValue());
  }

  private E exprFromNumberLiteral(NumberLiteral literal) {
    return factory.Data((float)literal.getNumber());
  }

  private E exprFromAssignment(Assignment assignment) {
    switch (assignment.getOperator()) {
      case Token.ASSIGN:
        return factory.Assign(
          exprFrom(assignment.getLeft()),
          exprFrom(assignment.getRight())
        );
      case Token.ASSIGN_ADD:
      case Token.ASSIGN_BITAND:
      case Token.ASSIGN_BITOR:
      case Token.ASSIGN_BITXOR:
      case Token.ASSIGN_DIV:
      case Token.ASSIGN_LSH:
      case Token.ASSIGN_MOD:
      case Token.ASSIGN_MUL:
      case Token.ASSIGN_RSH:
      case Token.ASSIGN_SUB:
      case Token.ASSIGN_URSH:
        return JSUtil.noimpl();
      default:
        return JSUtil.noimpl();
    }
  }

  private E exprFromForInLoop(ForInLoop loop) {
    if (loop.isForEach()) {
      return factory.Loop(
        JSUtil.mustIdentifierOf(loop.getIterator()),
        exprFrom(loop.getIteratedObject()),
        exprFrom(loop.getBody())
      );
    } else {
      return exprFromOther(loop);
    }
  }

  private E exprFromVariableDeclaration(
      VariableDeclaration decl,
      E innerScope) {
    if (decl.isStatement()) {
      List<VariableInitializer> inits = decl.getVariables();
      E result = innerScope;
      for (VariableInitializer init : inits) {
        E initialValue = init.getInitializer() != null
          ? exprFrom(init.getInitializer())
          : JSUtil.<E>noimpl();
        result = factory.Let(
          JSUtil.identifierOf(init.getTarget()),
          initialValue,
          result
        );
      }
      return result;
    } else {
      String identifier = JSUtil.identifierOf(decl);
      if (identifier == null) {
        return exprFromOther(decl);
      } else {
        return factory.Var(identifier);
      }
    }
  }

  private E exprFromIfStatement(IfStatement ifStmt) {
    // TODO: work for non boolean conditionals,
    // like strings, numbers, undefined, null, and objects.
    return factory.If(
      exprFrom(ifStmt.getCondition()),
      exprFrom(ifStmt.getThenPart()),
      exprFrom(ifStmt.getElsePart())
    );
  }

  private E exprFromFunctionNode(FunctionNode func) {
    switch (func.getParams().size()) {
      case 1:
        return factory.Fun(
          JSUtil.mustIdentifierOf(func.getParams().get(0)),
          exprFrom(func.getBody())
        );
      default:
        return JSUtil.noimpl();
    }
  }

  private E exprFromReturnStatement(ReturnStatement ret) {
    // Currently assuming inside function definition within batch block
    // TODO: make other if not in function definition within batch block
    // TODO: no return value
    return factory.setExtra(exprFrom(ret.getReturnValue()), new MarkedReturn());
  }

  private E exprFromOther(AstNode node) {
    return JSUtil.noimpl();
  }
}
