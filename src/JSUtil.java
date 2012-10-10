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
}
