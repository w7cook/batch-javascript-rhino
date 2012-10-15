import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.partition.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class JSToPartition<E> {
  private PartitionFactory<E> factory;
  private String root;

  public JSToPartition(PartitionFactory<E> factory, String root) {
    this.factory = factory;
    this.root = root;
  }

  public E exprFrom(AstNode node) {
    switch (node.getType()) {
      case Token.BLOCK:
        return exprFromScope((Scope)node);
      case Token.CALL:
        return exprFromFunctionCall((FunctionCall)node);
      case Token.EXPR_RESULT:
      case Token.EXPR_VOID:
        return exprFromExpressionStatement((ExpressionStatement)node);
      case Token.NAME:
        return exprFromName((Name)node);
      // Binary operators
      // Note: AND and OR are not listed here, since they carry a different
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
      default:
        System.out.println("INCOMPLETE: "+Token.typeToName(node.getType())+" "+node.getClass().getName());
        return noimpl();
    }
  }

  private List<E> mapExprFrom(List<AstNode> nodes) {
    List<E> exprs = new ArrayList<E>(nodes.size());
    for (AstNode node : nodes) {
      exprs.add(exprFrom(node));
    }
    return exprs;
  }

  private E convertSequence(Iterator<AstNode> nodes) {
    LinkedList<E> sequence = new LinkedList<E>();
    while (nodes.hasNext()) {
      AstNode node = nodes.next();
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

  private <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }

  private E exprFromScope(Scope scope) {
    return convertSequence(scope.getStatements().iterator());
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
      default:
        return noimpl();
    }
  }

  private E exprFromName(Name nameNode) {
    String name = nameNode.getIdentifier();
    if (name.equals(root)) { // TODO: inner scopes
      return factory.Var(factory.RootName());
    } else if (name.equals(factory.RootName())) {
      return noimpl(); // TODO: avoid collisions
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
        return noimpl();
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
        return noimpl();
      default:
        return noimpl();
    }
  }

  private E exprFromForInLoop(ForInLoop loop) {
    if (loop.isForEach()) {
      return factory.Loop(
        mustIdentifierOf(loop.getIterator()),
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
          : this.<E>noimpl();
        result = factory.Let(
          identifierOf(init.getTarget()),
          initialValue,
          result
        );
      }
      return result;
    } else {
      String identifier = identifierOf(decl);
      if (identifier == null) {
        return exprFromOther(decl);
      } else {
        return factory.Var(identifier);
      }
    }
  }

  private E exprFromOther(AstNode node) {
    return noimpl();
  }

  private String identifierOf(AstNode node) {
    switch (node.getType()) {
      case Token.NAME:
        return ((Name)node).getIdentifier();
      case Token.VAR:
        VariableDeclaration decl = (VariableDeclaration)node;
        List<VariableInitializer> inits = decl.getVariables();
        if (inits.size() == 1) {
          VariableInitializer init = inits.get(0);
          if (init.getInitializer() == null && !init.isDestructuring()) {
            return identifierOf(init.getTarget());
          } else {
            return null;
          }
        } else {
          return null;
        }
      default:
        return null;
    }
  }

  private String mustIdentifierOf(AstNode node) {
    String identifier = identifierOf(node);
    if (identifier == null) {
      return noimpl();
    }
    return identifier;
  }
}
