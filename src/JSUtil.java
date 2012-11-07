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
      addChild(new ExpressionStatement(genDeclare(_var, _expression)));
      addChild(_body);
    }};
  }

  public static VariableDeclaration genDeclare(
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
      default:
        return new ExpressionStatement(node);
    }
  }

  public static Block genBlock(final AstNode _node) {
    switch (_node.getType()) {
      case Token.BLOCK:
        return (Block)_node;
      default:
        return new Block() {{
          addStatement(JSUtil.genStatement(_node));
        }};
    }
  }

  public static Block appendToBlock(AstNode maybeBlock, AstNode node) {
    Block block = JSUtil.genBlock(maybeBlock);
    if (!JSUtil.isEmpty(node)) {
      block.addStatement(JSUtil.genStatement(node));
    }
    return block;
  }

  public static Block prependToBlock(AstNode node, AstNode maybeBlock) {
    Block block = JSUtil.genBlock(maybeBlock);
    if (!JSUtil.isEmpty(node)) {
      block.addChildToFront(JSUtil.genStatement(node));
    }
    return block;
  }

  public static boolean isEmpty(AstNode node) {
    switch (node.getType()) {
      case Token.EMPTY:
        return true;
      case Token.EXPR_VOID:
      case Token.EXPR_RESULT:
        System.out.println("HIT");
        return JSUtil.isEmpty(((ExpressionStatement)node).getExpression());
      default:
        return false;
    }
  }
}
