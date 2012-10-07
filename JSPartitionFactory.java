import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.partition.*;

import java.util.List;

public class JSPartitionFactory extends PartitionFactoryHelper<AstNode> {

  public final static String INPUT_NAME = "INPUT";
  public final static String OUTPUT_NAME = "OUTPUT";

  @Override
  public AstNode Var(String name) {
    return new Name(0, name);
  }

  @Override
  public AstNode Data(final Object _value) {
    if (_value instanceof String) {
      return new StringLiteral() {{
        setValue((String)_value);
        setQuoteCharacter('"');
      }};
    } else if (_value instanceof Float) {
      return new NumberLiteral(((Float)_value).doubleValue());
    } else {
      return noimpl();
    }
  }

  @Override
  public AstNode Fun(final String _var, final AstNode _body) {
    return new FunctionNode() {{
      addParam(new Name(0, _var));
      setBody(_body);
    }};
  }

  @Override
  public AstNode Prim(Op op, final List<AstNode> _args) {
    int type;
    if (op == Op.SEQ) {
      return new Block() {{
        for (AstNode node : _args) {
          addStatement(node);
        }
      }};
    }
    switch (_args.size()) {
      case 1:
        switch (op) {
          case NOT:
        }
        return noimpl();
      case 2:
        switch (op) {
          case ADD: type = Token.ADD; break;
          case SUB: type = Token.SUB; break;
          case MUL: type = Token.MUL; break;
          case DIV: type = Token.DIV; break;
          case MOD: type = Token.MOD; break;
          case NE:  type = Token.NE;  break;
          case EQ:  type = Token.EQ;  break;
          case LT:  type = Token.LT;  break;
          case GT:  type = Token.GT;  break;
          case LE:  type = Token.LE;  break;
          case GE:  type = Token.GE;  break;
          default:
            return noimpl();
        }
        return new InfixExpression(type, _args.get(0), _args.get(1), 0);
      default:
        return noimpl();
    }
  }

  @Override
  public AstNode Prop(AstNode base, String field) {
    return new PropertyGet(base, new Name(0, field));
  }

  @Override
  public AstNode Assign(AstNode target, AstNode source) {
    return new Assignment(Token.ASSIGN, target, source, 0);
  }

  @Override
  public AstNode Let(
      final String _var,
      final AstNode _expression,
      final AstNode _body) {
    return new Scope() {{
      addChild(new VariableDeclaration() {{
        addVariable(new VariableInitializer() {{
          setNodeType(Token.VAR);
          setTarget(new Name(0, _var));
          setInitializer(_expression);
        }});
      }});
      addChild(_body);
    }};
  }

  @Override
  public AstNode If(
      final AstNode _condition,
      final AstNode _thenExp,
      final AstNode _elseExp) {
    return new IfStatement() {{
      setCondition(_condition);
      setThenPart(_thenExp);
      setElsePart(_elseExp);
    }};
  }

  @Override
  public AstNode Loop(
      final String _var,
      final AstNode _collection,
      final AstNode _body) {
    final String unusedIncr = _var+"_i";
    return new ForLoop() {{
      setInitializer(new VariableDeclaration() {{
        addVariable(new VariableInitializer() {{
          setNodeType(Token.VAR);
          setTarget(new Name(0, unusedIncr));
          setInitializer(new NumberLiteral(0.0));
        }});
      }});
      setCondition(new InfixExpression(
        Token.LT,
        new Name(0, unusedIncr),
        Prop(_collection, "length"),
        0
      ));
      setIncrement(new UnaryExpression(Token.INC, 0, new Name(0, unusedIncr)));
      setBody(Let(
        _var,
        new ElementGet(_collection, new Name(0, unusedIncr)),
        _body
      ));
    }};
  }

  @Override
  public AstNode Call(AstNode target, String method, List<AstNode> args) {
    return DynamicCall(target, method, args);
  }

  @Override
  public AstNode In(String location) {
    return new PropertyGet(new Name(0, INPUT_NAME), new Name(0, location));
  }

  @Override
  public AstNode Out(String location, AstNode expression) {
    return new Assignment(
      Token.ASSIGN,
      new PropertyGet(new Name(0, OUTPUT_NAME), new Name(0, location)),
      expression,
      0
    );
  }

  @Override
  public AstNode Other(Object external, List<AstNode> subs) {
    return noimpl();
  }

  @Override
  public AstNode DynamicCall(
      final AstNode _target,
      final String _method,
      final List<AstNode> _args) {
    return new FunctionCall() {{
      setTarget(new PropertyGet(_target, new Name(0, _method)));
      setArguments(_args);
    }};
  }

  @Override
  public AstNode Mobile(String type, AstNode exp) {
    return noimpl();
  }

  @Override
  public AstNode setExtra(AstNode exp, Object extra) {
    if (extra == null) {
      return exp;
    }
    return noimpl();
  }

  @Override
  public AstNode Root() {
    return Var(ROOT_VAR_NAME);
  }

  @Override
  public AstNode Skip() {
    return Prim(Op.SEQ);
  }

  private <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
