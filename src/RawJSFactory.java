import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.partition.*;

import java.util.Iterator;
import java.util.List;

public class RawJSFactory extends PartitionFactoryHelper<Generator> {

  @Override
  public Generator Var(String name) {
    return Generator.Return(JSUtil.genName(name));
  }

  @Override
  public Generator Data(Object value) {
    AstNode node;
    if (value == null) {
      node = null;
    } else if (value instanceof String) {
      node = JSUtil.genStringLiteral((String)value, '"');
    } else if (value instanceof Float) {
      node = new NumberLiteral(((Float)value).doubleValue());
    } else {
      node = JSUtil.noimpl();
    }
    return Generator.Return(node);
  }

  @Override
  public Generator Fun(final String _var, final Generator _body) {
    return new Generator() {
      public AstNode Generate(
          final String _in,
          final String _out,
          final Function<AstNode, AstNode> _returnFunction) {
        return new FunctionNode() {{
          addParam(JSUtil.genName(_var));
          setBody(JSUtil.genBlock(
            _body.Generate(
              _in,
              _out,
              new Function<AstNode, AstNode>() {
                public AstNode call(final AstNode _result) {
                  return new ReturnStatement() {{
                    setReturnValue(_result);
                  }};
                }
              }
            )
          ));
        }};
      }
    };
  }

  private Generator Seq(final Iterator<Generator> _gens) {
    if (_gens.hasNext()) {
      return _gens.next().Bind(new Function<AstNode,Generator>() {
        public Generator call(AstNode node) {
          return new SequenceGenerator(node).Bind(
            Function.<AstNode,Generator>Const(Seq(_gens))
          );
        }
      });
    } else {
      return new SequenceGenerator();
    }
  }

  @Override
  public Generator Prim(final Op _op, List<Generator> argGens) {
    if (_op == Op.SEQ) {
      return Seq(argGens.iterator());
    }
    return Monad.SequenceBind(argGens, new JSGenFunction<List<AstNode>>() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction,
          List<AstNode> args) {
        int type;
        switch (args.size()) {
          case 1:
            switch (_op) {
              case NOT:
            }
            return JSUtil.noimpl();
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
                return JSUtil.noimpl();
            }
            return
              new InfixExpression(
                type,
                args.get(0),
                args.get(1),
                0
              );
          default:
            return JSUtil.noimpl();
        }
      }
    });
  }

  @Override
  public Generator Prop(Generator baseGen, final String _field) {
    return baseGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction,
          AstNode base) {
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
            Function<AstNode, AstNode> returnFunction,
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
      final Generator _bodyGen) {
    return expressionGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction,
          AstNode expression) {
        return JSUtil.genLet(
          _var,
          expression,
          _bodyGen.Generate(in, out, returnFunction)
        );
      }
    });
  }

  @Override
  public Generator If(
      final Generator _conditionGen,
      final Generator _thenExprGen,
      final Generator _elseExprGen) {
    return new Generator() {
      public AstNode Generate(
          final String _in,
          final String _out,
          final Function<AstNode, AstNode> _returnFunction) {
        final Generator _this = this;
        return _conditionGen.Bind(new JSGenFunction<AstNode>() {
          public AstNode Generate(
              final String _in,
              final String _out,
              final Function<AstNode, AstNode> _returnFunction,
              final AstNode _condition) {
            final AstNode _thenExpr =
              _thenExprGen.Generate(_in,_out,_returnFunction);
            final AstNode _elseExpr =
              _elseExprGen.Generate(_in,_out,_returnFunction);
            if (JSMarkers.IF_STATEMENT.equals(_this.extraInfo)) {
              return new IfStatement() {{
                setCondition(_condition);
                setThenPart(_thenExpr);
                if (!JSUtil.isEmpty(_elseExpr)) {
                  setElsePart(_elseExpr);
                }
              }};
            } else {
              return new ConditionalExpression() {{
                setTestExpression(_condition);
                setTrueExpression(_thenExpr);
                setFalseExpression(_elseExpr);
              }};
            }
          }
        }).Generate(_in, _out, _returnFunction);
      }
    };
  }

  @Override
  public Generator Loop(
      final String _var,
      Generator collectionGen,
      final Generator _bodyGen) {
    return collectionGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(
          final String _in,
          final String _out,
          final Function<AstNode, AstNode> _returnFunction,
          final AstNode _collection) {
        return new ForInLoop() {{
          setIterator(JSUtil.genDeclareExpr(_var));
          setIteratedObject(_collection);
          setBody(_bodyGen.Generate(_in, _out, _returnFunction));
        }};
      }
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
              Function<AstNode, AstNode> returnFunction,
              List<AstNode> args) {
            return JSUtil.genCall(_target, _method, args);
          }
        });
      }
    });
  }

  @Override
  public Generator In(String location) {
    throw new Error("Raw JS should not contain remote expressions");
  }

  @Override
  public Generator Out(String location, Generator expressionGen) {
    throw new Error("Raw JS should not contain remote expressions");
  }

  @Override
  public Generator Other(
      final Object _external,
      List<Generator> subGens) {
    if (_external.equals(Token.RETURN) && subGens.size() == 1) {
      return subGens.get(0).Bind(new Function<AstNode, Generator>() {
        public Generator call(AstNode result) {
          return new ReturnGenerator(result);
        }
      });
    } else {
      return new Generator() {
        public AstNode Generate(
            String in,
            String out,
            Function<AstNode, AstNode> returnFunction) {
          // generate subGens here
          return JSUtil.noimpl();
        }
      };
    }
  }

  @Override
  public Generator DynamicCall(
      Generator targetGen,
      final String _method,
      final List<Generator> _argGens) {
    return targetGen.Bind(new Function<AstNode,Generator>() {
      public Generator call(AstNode target) {
        if (JSUtil.isEmpty(target)) {
          return Monad.SequenceBind(
            _argGens,
            new JSGenFunction<List<AstNode>>() {
              public AstNode Generate(
                  String in,
                  String out,
                  Function<AstNode, AstNode> returnFunction,
                  List<AstNode> args) {
                return JSUtil.genCall(JSUtil.genName(_method), args);
              }
            }
          );
        } else {
          return JSUtil.noimpl();
        }
      }
    });
  }

  @Override
  public Generator Mobile(String type, Generator exp) {
    return new Generator() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction) {
        return JSUtil.noimpl();
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
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction) {
        return JSUtil.genName(ROOT_VAR_NAME);
      }
    };
  }

  @Override
  public Generator Skip() {
    return Generator.Return(JSUtil.concatBlocks());
  }
}
