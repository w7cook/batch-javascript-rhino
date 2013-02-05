import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.List;

public class JSUtil {

  public static Name genName(String identifier) {
    return new Name(0, identifier);
  }

  public static StringLiteral genStringLiteral(String s) {
    return genStringLiteral(s, '"');
  }

  public static StringLiteral genStringLiteral(
      final String _s,
      final char _quote) {
    return new StringLiteral() {{
      setValue(_s);
      setQuoteCharacter(_quote);
    }};
  }

  public static AstNode genLet(
      final String _var,
      final AstNode _expression,
      final AstNode _body) {
    return new Scope() {{
      addChild(genDeclare(_var, _expression));
      addChild(_body);
    }};
  }

  public static VariableDeclaration genDeclareExpr(
      final String _var,
      final AstNode _init) {
    return new VariableDeclaration() {{
      addVariable(new VariableInitializer() {{
        setNodeType(Token.VAR);
        setTarget(new Name(0, _var));
        setInitializer(_init);
      }});
    }};
  }

  public static VariableDeclaration genDeclare(String var, AstNode init) {
    VariableDeclaration declareExpr = genDeclareExpr(var, init);
    declareExpr.setIsStatement(true);
    return declareExpr;
    // Don't want to wrap in ExpressionStatement, ends up with double semicolons
    //return new ExpressionStatement(declareExpr);
  }

  public static AstNode genCall(
      final AstNode _target,
      final String _method,
      final List<AstNode> _args) {
    return new FunctionCall() {{
      setTarget(new PropertyGet(_target, genName(_method)));
      setArguments(_args);
    }};
  }

  public static AstNode genStatement(AstNode node) {
    switch (node.getType()) {
      case Token.BLOCK:
      case Token.EXPR_VOID:
      case Token.EXPR_RESULT:
      case Token.IF:
        return node;
      case Token.VAR:
        if (((VariableDeclaration)node).isStatement()) {
          return node;
        }
        // fallthrough
      default:
        return new ExpressionStatement(node);
    }
  }

  public static Block genBlock(final AstNode _node) {
    if (_node instanceof Block) {
      return (Block)_node;
    } else {
      return new Block() {{
        addStatement(JSUtil.genStatement(_node));
      }};
    }
  }

  public static Block concatBlocks(AstNode node1, AstNode node2) {
    Block b = new Block();
    if (!JSUtil.isEmpty(node1)) {
      for (Node stmt : JSUtil.genBlock(node1)) {
        b.addStatement((AstNode)stmt);
      }
    }
    if (!JSUtil.isEmpty(node2)) {
      for (Node stmt : JSUtil.genBlock(node2)) {
        b.addStatement((AstNode)stmt);
      }
    }
    return b;
  }

  public static boolean isEmpty(AstNode node) {
    switch (node.getType()) {
      case Token.BLOCK:
        for (Node inner : node) {
          if (!isEmpty((AstNode)inner)) {
            return false;
          }
        }
        return true;
      case Token.EMPTY:
        return true;
      case Token.EXPR_VOID:
      case Token.EXPR_RESULT:
        return JSUtil.isEmpty(((ExpressionStatement)node).getExpression());
      default:
        return false;
    }
  }
}
