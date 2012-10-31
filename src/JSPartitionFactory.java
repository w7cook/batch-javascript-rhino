import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.partition.*;

import java.util.ArrayList;
import java.util.List;

public class JSPartitionFactory 
    extends PartitionFactoryHelper<JSGenerator> {

  public final static String INPUT_NAME = "INPUT";
  public final static String OUTPUT_NAME = "OUTPUT";

  @Override
  public JSGenerator Var(final String _name) {
    return JSGenerator.Return(JSUtil.genName(_name));
  }

  @Override
  public JSGenerator Data(final Object _value) {
    AstNode node;
    if (_value instanceof String) {
      node = JSUtil.genStringLiteral((String)_value, '"');
    } else if (_value instanceof Float) {
      node = new NumberLiteral(((Float)_value).doubleValue());
    } else {
      node = noimpl();
    }
    return JSGenerator.Return(node);
  }

  @Override
  public JSGenerator Fun(final String _var, final JSGenerator _body) {
    return new JSGenerator() {
      public AstNode Generate(final String _in, final String _out) {
        return new FunctionNode() {{
          addParam(JSUtil.genName(_var));
          setBody(_body.Generate(_in, _out));
        }};
      }
    };
  }

  @Override
  public JSGenerator Prim(final Op _op, List<JSGenerator> argGens) {
    return Monad.Sequence(argGens).Bind(new JSGenFunction<List<AstNode>>() {
      public AstNode Generate(
          final String _in,
          final String _out,
          final List<AstNode> _args) {
        int type;
        if (_op == Op.SEQ) {
          switch (_args.size()) {
            case 0:
              return new EmptyNode();
            case 1:
              return _args.get(0);
            default:
              return new Block() {{
                for (AstNode node : _args) {
                  // TODO: remove empty nodes that were asyncronized
                  addStatement(JSUtil.genStatement(node));
                }
              }};
          }
        }
        switch (_args.size()) {
          case 1:
            switch (_op) {
              case NOT:
            }
            return noimpl();
          case 2:
            switch (_op) {
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
            return
              new InfixExpression(
                type,
                _args.get(0),
                _args.get(1),
                0
              );
          default:
            return noimpl();
        }
      }
    });
  }

  @Override
  public JSGenerator Prop(JSGenerator baseGen, final String _field) {
    return baseGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(String in, String out, AstNode base) {
        return new PropertyGet(
          base,
          JSUtil.genName(_field)
        );
      }
    });
  }

  @Override
  public JSGenerator Assign(JSGenerator targetGen, JSGenerator sourceGen) {
    return Monad.Bind2(targetGen, sourceGen,
      new JSGenFunction2<AstNode, AstNode>() {
        public AstNode Generate(
            String in, 
            String out,
            AstNode target,
            AstNode source) {
          return new Assignment(Token.ASSIGN, target, source, 0);
        }
      }
    );
  }

  @Override
  public JSGenerator Let(
      final String _var,
      JSGenerator expressionGen,
      JSGenerator bodyGen) {
    return Monad.Bind2(expressionGen, bodyGen,
      new JSGenFunction2<AstNode,AstNode>() {
        public AstNode Generate(
            String in,
            String out,
            AstNode expression,
            AstNode body) {
          return JSUtil.genLet(_var, expression, body);
        }
      }
    );
  }

  @Override
  public JSGenerator If(
      JSGenerator conditionGen,
      JSGenerator thenExpGen,
      JSGenerator elseExpGen) {
    return Monad.Bind3(conditionGen, thenExpGen, elseExpGen,
      new JSGenFunction3<AstNode, AstNode, AstNode>() {
        public AstNode Generate(
            String in,
            String out,
            final AstNode _condition,
            final AstNode _thenExp,
            final AstNode _elseExp) {
          return new IfStatement() {{
            setCondition(_condition);
            setThenPart(JSUtil.genStatement(_thenExp));
            setElsePart(JSUtil.genStatement(_elseExp));
          }};
        }
      }
    );
  }

  @Override
  public JSGenerator Loop(
      final String _var,
      JSGenerator collectionGen,
      final JSGenerator _body) {
    // TODO: Async
    return collectionGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(
          final String _in,
          final String _out,
          AstNode collection) {
        final String index = _var+"_i";
        if (!(collection instanceof EmptyNode)) {
          return noimpl();
        }
        return new ForLoop() {{
          setInitializer(
            JSUtil.genDeclare(index, new NumberLiteral(0, "0"))
          );
          setCondition(new InfixExpression(
            Token.LT,
            JSUtil.genName(index),
            new PropertyGet(
              new PropertyGet(
                JSUtil.genName(_in),
                JSUtil.genName(_var)
              ),
              JSUtil.genName("length")
            ),
            0
          ));
          setIncrement(
            new UnaryExpression(Token.INC, 0, JSUtil.genName(index))
          );
          setBody(JSUtil.genLet(
            _var,
            new ElementGet(
              new PropertyGet(
                JSUtil.genName(_in),
                JSUtil.genName(_var)
              ),
              JSUtil.genName(index)
            ),
            _body.Generate(
              _in  != null ? _var : null,
              _out != null ? _var : null
            )
          ));
        }};
      }
    });
  }

  @Override
  public JSGenerator Call(
      JSGenerator targetGen,
      final String _method,
      List<JSGenerator> argGens) {
    return Monad.Bind2(targetGen, Monad.Sequence(argGens),
      new JSGenFunction2<AstNode, List<AstNode>>() {
        public AstNode Generate(
            String in,
            String out,
            AstNode target,
            List<AstNode> args) {
          return JSUtil.genCall(target, _method, args);
        }
      }
    );
  }

  @Override
  public JSGenerator In(final String _location) {
    return new JSGenerator() {
      public AstNode Generate(String in, String out) {
        return new PropertyGet(
          JSUtil.genName(in),
          JSUtil.genName(_location)
        );
      }
    };
  }

  @Override
  public JSGenerator Out(
      final String _location,
      JSGenerator expressionGen) {
    return expressionGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(String in, String out, AstNode expression) {
        return new Assignment(
          Token.ASSIGN,
          new PropertyGet(
            JSUtil.genName(out),
            JSUtil.genName(_location)
          ),
          expression,
          0
        );
      }
    });
  }

  @Override
  public JSGenerator Other(Object external, List<JSGenerator> subGens) {
    return Monad.Sequence(subGens).Bind(new JSGenFunction<List<AstNode>>() {
      public AstNode Generate(String in, String out, List<AstNode> subs) {
        return noimpl();
      }
    });
  }

  @Override
  public JSGenerator DynamicCall(
      JSGenerator target,
      String method,
      List<JSGenerator> args) {
    return new JSGenerator() {
      public AstNode Generate(String in, String out) {
        return noimpl();
      }
    };
  }

  @Override
  public JSGenerator Mobile(String type, JSGenerator exp) {
    return new JSGenerator() {
      public AstNode Generate(String in, String out) {
        return noimpl();
      }
    };
  }

  @Override
  public JSGenerator setExtra(JSGenerator exp, Object extra) {
    return exp.setExtra(extra);
  }

  @Override
  public JSGenerator Root() {
    return new JSGenerator() {
      public AstNode Generate(String in, String out) {
        return JSUtil.genName(ROOT_VAR_NAME);
      }
    };
  }

  @Override
  public JSGenerator Skip() {
    return JSGenerator.Return(new EmptyStatement());
  }
  // TODO: Double check this
  private class EmptyNode extends Block {}

  private static <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
