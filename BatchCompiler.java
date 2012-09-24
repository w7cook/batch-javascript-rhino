// WHAT IS: CodeModel.factory.allowAllTransers, f.Mobile(...),
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.partition.*;
import batch.Op;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class BatchCompiler implements NodeVisitor {
  public static void main(String[] args)
      throws FileNotFoundException, IOException {
    String fileName = args[0];
    FileReader reader = new FileReader(new File(fileName));
    Parser parser = new Parser();
      //new Parser(new CompilerEnvirons(), DefaultErrorReporter.instance);
    AstRoot ast = parser.parse(reader, fileName, /*linenumber*/ 0);
    BatchCompiler compiler = new BatchCompiler();
    ast.visit(compiler);
    if (compiler.compiledBatchNode != null) {
      // TODO: replace code
      System.out.println(compiler.compiledBatchNode.toSource());
    } else {
      System.out.println("No batch statement found");
    }
  }

  public AstNode compiledBatchNode;
  private String iterator;

  public boolean visit(AstNode node) {
    if (!(node instanceof BatchLoop)) {
      return true;
    }
    BatchLoop batch = (BatchLoop)node;
    Name name = null;
    switch (batch.getIterator().getType()) {
      case Token.VAR:
        name = (Name)((VariableDeclaration)batch.getIterator())
          .getVariables().get(0).getTarget();
        break;
      case Token.NAME:
        name = (Name)batch.getIterator();
        break;
      default:
        noimpl();
    }
    iterator = name.getIdentifier();
    CodeModel.factory.allowAllTransers = true;
    PExpr origExpr = exprFrom(CodeModel.factory, batch.getBody());
    System.out.println(origExpr);
    Environment env = new Environment(CodeModel.factory)
      .extend(CodeModel.factory.RootName(), null, Place.REMOTE);
    System.out.println(origExpr.partition(Place.MOBILE, env));
    compiledBatchNode = batch;
    return false;
  }

  private <E> E exprFrom(PartitionFactory<E> f, AstNode node) {
    switch (node.getType()) {
      case Token.BLOCK:
        return exprFrom(f, (Scope)node);
      case Token.CALL:
        return exprFrom(f, (FunctionCall)node);
      case Token.EXPR_RESULT:
      case Token.EXPR_VOID:
        return exprFrom(f, (ExpressionStatement)node);
      case Token.NAME:
        return exprFrom(f, (Name)node);
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
        return exprFrom(f, (InfixExpression)node);
      //case Token.NOT: // TODO: this has slightly different meaning in javascript
      //  return exprFrom(f, (UnaryExpression)node);
      case Token.STRING:
        return exprFrom(f, (StringLiteral)node);
      case Token.NUMBER:
        return exprFrom(f, (NumberLiteral)node);
    }
    System.out.println(Token.typeToName(node.getType()));
    return f.Skip();
  }

  private <E> E exprFrom(PartitionFactory<E> f, Scope scope) {
    return f.Prim(Op.SEQ, mapExprFrom(f, scope.getStatements()));
  }

  private <E> E exprFrom(PartitionFactory<E> f, ExpressionStatement statement) {
    return exprFrom(f, statement.getExpression());
  }

  private <E> E exprFrom(PartitionFactory<E> f, FunctionCall call) {
    AstNode target = call.getTarget();
    switch (target.getType()) {
      case Token.GETPROP:
        PropertyGet propGet = (PropertyGet)target;
        return f.Call(
          exprFrom(f, propGet.getTarget()),
          propGet.getProperty().getIdentifier(),
          mapExprFrom(f, call.getArguments())
        );
    }
    return noimpl();
  }

  private <E> E exprFrom(PartitionFactory<E> f, Name nameNode) {
    String name = nameNode.getIdentifier();
    if (name.equals(iterator)) {
      return f.Var(f.RootName());
    } else if (name.equals(f.RootName())) {
      return noimpl();
    } else {
      return f.Var(name);
    }
  }

  private <E> E exprFrom(PartitionFactory<E> f, InfixExpression infix) {
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
      case Token.AND:// binOp = Op.AND; break;
        return noimpl();
      case Token.OR: // binOp = Op.OR;  break;
        return noimpl();
      default:
        return noimpl();
    }
    return f.Prim(
      binOp, 
      exprFrom(f, infix.getLeft()),
      exprFrom(f, infix.getRight())
    );
  }

  private <E> E exprFrom(PartitionFactory<E> f, StringLiteral literal) {
    return f.Data(literal.getValue());
  }

  private <E> E exprFrom(PartitionFactory<E> f, NumberLiteral literal) {
    return f.Data((float)literal.getNumber());
  }

  private <E> List<E> mapExprFrom(PartitionFactory<E> f, List<AstNode> nodes) {
    List<E> exprs = new ArrayList<E>(nodes.size());
    for (AstNode node : nodes) {
      exprs.add(exprFrom(f, node));
    }
    return exprs;
  }

  private <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
