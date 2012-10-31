import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.partition.*;

import java.util.ArrayList;
import java.util.List;

public class JSPartitionFactory extends PartitionFactoryHelper<Generator> {

  public final static String INPUT_NAME = "INPUT";
  public final static String OUTPUT_NAME = "OUTPUT";

  @Override
  public Generator Var(final String _name) {
    return Generator.Return(JSUtil.genName(_name));
  }

  @Override
  public Generator Data(final Object _value) {
    AstNode node;
    if (_value instanceof String) {
      node = JSUtil.genStringLiteral((String)_value, '"');
    } else if (_value instanceof Float) {
      node = new NumberLiteral(((Float)_value).doubleValue());
    } else {
      node = noimpl();
    }
    return Generator.Return(node);
  }

  @Override
  public Generator Fun(final String _var, final Generator _body) {
    return new Generator() {
      public AstNode Generate(final String _in, final String _out) {
        return new FunctionNode() {{
          addParam(JSUtil.genName(_var));
          setBody(_body.Generate(_in, _out));
        }};
      }
    };
  }

  @Override
  public Generator Prim(final Op _op, List<Generator> argGens) {
    return Monad.SequenceBind(argGens, new JSGenFunction<List<AstNode>>() {
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
  public Generator Prop(Generator baseGen, final String _field) {
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
  public Generator Assign(Generator targetGen, Generator sourceGen) {
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
  public Generator Let(
      final String _var,
      Generator expressionGen,
      Generator bodyGen) {
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
  public Generator If(
      Generator conditionGen,
      Generator thenExpGen,
      Generator elseExpGen) {
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
  public Generator Loop(
      final String _var,
      Generator collectionGen,
      final Generator _bodyGen) {
    final String _next = _var+"_next";
    return collectionGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(
          final String _in,
          final String _out,
          AstNode collection) {
        if (!(collection instanceof EmptyNode)) {
          return noimpl();
        }
        return JSUtil.genCall(
          JSUtil.genName(_in),
          "asyncForEach",
          new ArrayList<AstNode>() {{
            add(JSUtil.genStringLiteral(_var));
            add(new FunctionNode() {{
              addParam(JSUtil.genName(_var));
              addParam(JSUtil.genName(_next));
              setBody(
                _bodyGen.Bind(new JSGenFunction<AstNode>() {
                  public AstNode Generate(
                      String in,
                      String out,
                      final AstNode _body) {
                    return new Block() {{
                      addStatement(JSUtil.genStatement(_body));
                      // TODO: fold down one
                      addStatement(JSUtil.genStatement(
                        new FunctionCall() {{
                          setTarget(JSUtil.genName(_next));
                        }}
                      ));
                    }};
                  }
                }).Generate(
                  _in  != null ? _var : null,
                  _out != null ? _var : null
                )
              );
            }});
            /*
            add(new FunctionNode() {{
              if (this.callbackGen) {
                setBody(this.callbackGen.Generate(_in, _out));
              }
            }});
            */
          }}
        );
      }

/*
      @Override
      public <R> Generator<R> Bind(
          final Function<A, ? extends MonadI<Generator, R>> _f,
          AstNode collection) {
        if (!(collection instanceof EmptyNode)) {
          return noimpl();
        }
      }
*/
    });
  }

  @Override
  public Generator Call(
      Generator targetGen,
      final String _method,
      final List<Generator> _argGens) {
    return targetGen.Bind(new Function<AstNode, Generator>() {
      public Generator call(final AstNode _target) {
        return Monad.SequenceBind(_argGens, new JSGenFunction<List<AstNode>>() {
          public AstNode Generate(
              String in,
              String out,
              List<AstNode> args) {
            return JSUtil.genCall(_target, _method, args);
          }
        });
      }
    });
  }

  @Override
  public Generator In(String location) {
    return new InGenerator(location);
  }

  @Override
  public Generator Out(final String _location, Generator expressionGen) {
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
  public Generator Other(
      Object external,
      List<Generator> subGens) {
    return Monad.SequenceBind(subGens, new JSGenFunction<List<AstNode>>() {
      public AstNode Generate(String in, String out, List<AstNode> subs) {
        return noimpl();
      }
    });
  }

  @Override
  public Generator DynamicCall(
      Generator target,
      String method,
      List<Generator> args) {
    return new Generator() {
      public AstNode Generate(String in, String out) {
        return noimpl();
      }
    };
  }

  @Override
  public Generator Mobile(String type, Generator exp) {
    return new Generator() {
      public AstNode Generate(String in, String out) {
        return noimpl();
      }
    };
  }

  @Override
  public Generator setExtra(Generator exp, Object extra) {
    return exp.setExtra(extra);
  }

  @Override
  public Generator Root() {
    return new Generator() {
      public AstNode Generate(String in, String out) {
        return JSUtil.genName(ROOT_VAR_NAME);
      }
    };
  }

  @Override
  public Generator Skip() {
    return Generator.Return(new EmptyStatement());
  }
  // TODO: Double check this
  private class EmptyNode extends Block {}

  private static <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
