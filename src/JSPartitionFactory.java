import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.partition.*;

import java.util.ArrayList;
import java.util.List;

public class JSPartitionFactory extends PartitionFactoryHelper<JSGenerator> {

  public final static String INPUT_NAME = "INPUT";
  public final static String OUTPUT_NAME = "OUTPUT";

  @Override
  public JSGenerator Var(final String _name) {
    return new JSGenerator() {
      public AstNode generateNode(String in, String out) {
        return JSUtil.genName(_name);
      }
    };
  }

  @Override
  public JSGenerator Data(final Object _value) {
    return new JSGenerator() {
      public AstNode generateNode(String in, String out) {
        if (_value instanceof String) {
          return JSUtil.genStringLiteral((String)_value, '"');
        } else if (_value instanceof Float) {
          return new NumberLiteral(((Float)_value).doubleValue());
        } else {
          return noimpl();
        }
      }
    };
  }

  @Override
  public JSGenerator Fun(final String _var, final JSGenerator _body) {
    return new JSGenerator() {
      public AstNode generateNode(final String _in, final String _out) {
        return new FunctionNode() {{
          addParam(JSUtil.genName(_var));
          setBody(_body.generateNode(_in, _out));
        }};
      }
    };
  }

  @Override
  public JSGenerator Prim(final Op _op, final List<JSGenerator> _args) {
    return new JSGenerator() {
      public AstNode generateNode(final String _in, final String _out) {
        int type;
        final List<AstNode> argNodes = new ArrayList<AstNode>() {{
          for (JSGenerator gen : _args) {
            add(gen.generateNode(_in, _out));
          }
        }};
        if (_op == Op.SEQ) {
          switch (argNodes.size()) {
            case 0:
              return new EmptyNode();
            case 1:
              return argNodes.get(0);
            default:
              return new Block() {{
                for (AstNode node : argNodes) {
                  addStatement(node);
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
              new InfixExpression(type, argNodes.get(0), argNodes.get(1), 0);
          default:
            return noimpl();
        }
      }
    };
  }

  @Override
  public JSGenerator Prop(final JSGenerator _base, final String _field) {
    return new JSGenerator() {
      public AstNode generateNode(String in, String out) {
        return new PropertyGet(
          _base.generateNode(in, out),
          JSUtil.genName(_field)
        );
      }
    };
  }

  @Override
  public JSGenerator Assign(
      final JSGenerator _target,
      final JSGenerator _source) {
    return new JSGenerator() {
      public AstNode generateNode(String in, String out) {
        return new Assignment(
          Token.ASSIGN,
          _target.generateNode(in, out),
          _source.generateNode(in, out),
          0
        );
      }
    };
  }

  @Override
  public JSGenerator Let(
      final String _var,
      final JSGenerator _expression,
      final JSGenerator _body) {
    return new JSGenerator() {
      public AstNode generateNode(final String _in, final String _out) {
        return JSUtil.genLet(
          _var,
          _expression.generateNode(_in, _out),
          _body.generateNode(_in, _out)
        );
      }
    };
  }

  @Override
  public JSGenerator If(
      final JSGenerator _condition,
      final JSGenerator _thenExp,
      final JSGenerator _elseExp) {
    return new JSGenerator() {
      public AstNode generateNode(final String _in, final String _out) {
        return new IfStatement() {{
          setCondition(_condition.generateNode(_in, _out));
          setThenPart(_thenExp.generateNode(_in, _out));
          setElsePart(_elseExp.generateNode(_in, _out));
        }};
      }
    };
  }

  @Override
  public JSGenerator Loop(
      final String _var,
      final JSGenerator _collection,
      final JSGenerator _body) {
    return new JSGenerator() {
      public AstNode generateNode(final String _in, final String _out) {
        final String index = _var+"_i";
        if (!(_collection.generateNode(_in, _out) instanceof EmptyNode)) {
          return noimpl();
        }
        //  _collectionGen = new Gen<AstNode>() {
        //    public AstNode gen() {
        //      return new PropertyGet(
        //        new PropertyGet(
        //          JSUtil.genName(_in),
        //          JSUtil.genName("iterations")
        //        ),
        //        JSUtil.genName(_var)
        //      );
        //    }
        //  };
        return new ForLoop() {{
          setInitializer(
            JSUtil.genDeclare(index, new NumberLiteral(0.0))
          );
          setCondition(new InfixExpression(
            Token.LT,
            JSUtil.genName(index),
            JSUtil.genCall(
              JSUtil.genCall(
                JSUtil.genName(_in),
                "getIteration",
                new ArrayList<AstNode>() {{
                  add(JSUtil.genStringLiteral(_var));
                }}
              ),
              "getNumberOfIterations",
              new ArrayList<AstNode>()
            ),
            //new PropertyGet(_collectionGen.gen(), JSUtil.genName("length")),
            0
          ));
          setIncrement(
            new UnaryExpression(Token.INC, 0, JSUtil.genName(index))
          );
          setBody(JSUtil.genLet(
            _var,
            //new ElementGet(
            //  _collectionGen.gen(),
            //  JSUtil.genName(index)
            //),
            JSUtil.genCall(
              JSUtil.genCall(
                JSUtil.genName(_in),
                "getIteration",
                new ArrayList<AstNode>() {{
                  add(JSUtil.genStringLiteral(_var));
                }}
              ),
              "getIteration",
              new ArrayList<AstNode>() {{
                add(JSUtil.genName(index));
              }}
            ),
            _body.generateNode(
              _in  != null ? _var : null,
              _out != null ? _var : null
            )
          ));
        }};
      }
    };
  }

  @Override
  public JSGenerator Call(
      final JSGenerator _target,
      final String _method,
      final List<JSGenerator> _args) {
    return new JSGenerator() {
      public AstNode generateNode(final String _in, final String _out) {
        return JSUtil.genCall(
          _target.generateNode(_in, _out),
          _method,
          new ArrayList<AstNode>() {{
            for (JSGenerator _arg : _args) {
              add(_arg.generateNode(_in, _out));
            }
          }}
        );
      }
    };
  }

  @Override
  public JSGenerator In(final String _location) {
    return new JSGenerator() {
      public AstNode generateNode(String in, String out) {
        //return new PropertyGet(
        //  new PropertyGet(JSUtil.genName(in), JSUtil.genName("values")),
        //  JSUtil.genName(_location)
        //);
        return JSUtil.genCall(
          JSUtil.genName(in),
          "get",
          new ArrayList<AstNode>() {{
            add(JSUtil.genStringLiteral(_location));
          }}
        );
      }
    };
  }

  @Override
  public JSGenerator Out(
      final String _location,
      final JSGenerator _expression) {
    return new JSGenerator() {
      public AstNode generateNode(final String _in, final String _out) {
        //return new Assignment(
        //  Token.ASSIGN,
        //  new PropertyGet(
        //    new PropertyGet(JSUtil.genName(out), JSUtil.genName("values")),
        //    JSUtil.genName(_location)
        //  ),
        //  _expression.generateNode(in, out),
        //  0
        //);
        return JSUtil.genCall(
          JSUtil.genName(_out),
          "put",
          new ArrayList<AstNode>() {{
            add(JSUtil.genStringLiteral(_location));
            add(_expression.generateNode(_in, _out));
          }}
        );
      }
    };
  }

  @Override
  public JSGenerator Other(Object external, List<JSGenerator> subs) {
    return new JSGenerator() {
      public AstNode generateNode(String in, String out) {
        return noimpl();
      }
    };
  }

  @Override
  public JSGenerator DynamicCall(
      JSGenerator target,
      String method,
      List<JSGenerator> args) {
    return new JSGenerator() {
      public AstNode generateNode(String in, String out) {
        return noimpl();
      }
    };
  }

  @Override
  public JSGenerator Mobile(String type, JSGenerator exp) {
    return new JSGenerator() {
      public AstNode generateNode(String in, String out) {
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
      public AstNode generateNode(String in, String out) {
        return JSUtil.genName(ROOT_VAR_NAME);
      }
    };
  }

  @Override
  public JSGenerator Skip() {
    return Prim(Op.SEQ);
  }
  private class EmptyNode extends Block {}

  private static <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
