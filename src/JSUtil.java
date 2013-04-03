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

  public static VariableDeclaration genDeclareExpr(final String _var) {
    return new VariableDeclaration() {{
      addVariable(new VariableInitializer() {{
        setNodeType(Token.VAR);
        setTarget(JSUtil.genName(_var));
      }});
    }};
  }

  public static VariableDeclaration genDeclareExpr(
      final String _var,
      final AstNode _init) {
    return new VariableDeclaration() {{
      addVariable(new VariableInitializer() {{
        setNodeType(Token.VAR);
        setTarget(JSUtil.genName(_var));
        setInitializer(_init);
      }});
    }};
  }

  public static VariableDeclaration genDeclare(String var) {
    VariableDeclaration declareExpr = genDeclareExpr(var);
    declareExpr.setIsStatement(true);
    // Don't want to wrap in ExpressionStatement, ends up with double semicolons
    return declareExpr;
  }

  public static VariableDeclaration genDeclare(String var, AstNode init) {
    VariableDeclaration declareExpr = genDeclareExpr(var, init);
    declareExpr.setIsStatement(true);
    // Don't want to wrap in ExpressionStatement, ends up with double semicolons
    return declareExpr;
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

  public static AstNode genCall(
      final AstNode _target,
      final String _method,
      final AstNode... _args) {
    return new FunctionCall() {{
      setTarget(new PropertyGet(_target, genName(_method)));
      for (AstNode arg : _args) {
        addArgument(arg);
      }
    }};
  }

  public static AstNode genCall(
      final AstNode _function,
      final AstNode... _args) {
    return new FunctionCall() {{
      setTarget(_function);
      for (AstNode arg : _args) {
        addArgument(arg);
      }
    }};
  }

  public static AstNode genCall(
      final AstNode _function,
      final List<AstNode> _args) {
    return new FunctionCall() {{
      setTarget(_function);
      setArguments(_args);
    }};
  }

  public static AstNode genArray(final List<AstNode> _args) {
    return new ArrayLiteral() {{
      setElements(_args);
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

  public static Block concatBlocks(AstNode... nodes) {
    Block b = new Block();
    for (AstNode node : nodes) {
      if (!JSUtil.isEmpty(node)) {
        for (Node stmt : JSUtil.genBlock(node)) {
          b.addStatement((AstNode)stmt);
        }
      }
    }
    return b;
  }

  public static boolean isEmpty(AstNode node) {
    if (node == null) {
      return true;
    }
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

  public static InfixExpression genInfix(int type, AstNode... args) {
    InfixExpression ie = new InfixExpression(
      type,
      args[0],
      args[1],
      0
    );
    for (int i=2; i<args.length; i++) {
      ie = new InfixExpression(
        type,
        ie,
        args[i],
        0
      );
    }
    return ie;
  }

  public static KeywordLiteral genTrue() {
    return new KeywordLiteral().setType(Token.TRUE);
  }

  public static KeywordLiteral genFalse() {
    return new KeywordLiteral().setType(Token.FALSE);
  }

  public static Name genUndefined() {
    return JSUtil.genName("undefined");
  }

  public static ObjectLiteral genObject(List<AstNode> elements) {
    ObjectLiteral obj = new ObjectLiteral();
    for (AstNode node : elements) {
      obj.addElement((ObjectProperty)node);
    }
    return obj;
  }

  public static ObjectProperty genProperty(String key, AstNode value) {
    ObjectProperty prop = new ObjectProperty();
    prop.setNodeType(Token.COLON);
    prop.setLeft(JSUtil.genName(key));
    prop.setRight(value);
    return prop;
  }

  public static String identifierOf(AstNode node) {
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

  public static String mustIdentifierOf(AstNode node) {
    String identifier = identifierOf(node);
    if (identifier == null) {
      return noimpl();
    }
    return identifier;
  }

  public static <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
