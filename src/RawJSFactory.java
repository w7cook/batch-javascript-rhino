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
    } else if (value instanceof Number) {
      node = new NumberLiteral(((Number)value).doubleValue());
    } else if (value instanceof Boolean) {
      node = JSUtil.genBoolean((Boolean)value);
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
          case 0:
            return JSUtil.noimpl();
          case 1:
            switch (_op) {
              case NOT: type = Token.NOT; break;
              default: return JSUtil.noimpl();
            }
            return JSUtil.genUnary(type, args.get(0));
          default:
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
              case AND: type = Token.AND; break;
              case OR:  type = Token.OR;  break;
              default:
                return JSUtil.noimpl();
            }
            return JSUtil.genInfix(type, args.toArray(new AstNode[]{}));
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
    Generator gen = expressionGen.Bind(new JSGenFunction<AstNode>() {
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
    return
      new MarkedGenerator(gen, JSMarkers.LET, _var, expressionGen, _bodyGen);
  }

  @Override
  public Generator If(
      final Generator _conditionGen,
      final Generator _thenExprGen,
      final Generator _elseExprGen) {
    Generator gen = new Generator() {
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
            if (_this.extras.get(JSMarkers.IF_STATEMENT) == true) {
              return new IfStatement() {{
                setCondition(_condition);
                setThenPart(_thenExpr);
                if (!JSUtil.isEmpty(_elseExpr)) {
                  setElsePart(_elseExpr);
                }
              }};
            } else {
              return JSUtil.genConditionalExpression(
                _condition,
                _thenExpr,
                _elseExpr
              );
            }
          }
        }).Generate(_in, _out, _returnFunction);
      }
    };
    return new MarkedGenerator(
      gen,
      JSMarkers.IF,
      _conditionGen,
      _thenExprGen,
      _elseExprGen
    );
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
        final String _i = _var+"_$i";
        final String _col = _var+"_$col";
        return new ForLoop() {{
          setInitializer(
            JSUtil.genDeclareExpr(
              new Pair<String, AstNode>(_i, JSUtil.genNumberLiteral(0)),
              new Pair<String, AstNode>(_col, _collection)
            )
          );
          setCondition(
            JSUtil.genInfix(
              Token.LT, 
              JSUtil.genName(_i),
              JSUtil.genPropertyGet(JSUtil.genName(_col), "length")
            )
          );
          setIncrement(JSUtil.genUnary(Token.INC, JSUtil.genName(_i)));
          setBody(
            JSUtil.concatBlocks(
              JSUtil.genDeclareExpr(
                _var,
                JSUtil.genElementGet(JSUtil.genName(_col), JSUtil.genName(_i))
              ),
              _bodyGen.Generate(_in, _out, _returnFunction)
            )
          );
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
      Object external,
      List<Generator> subGens) {
    if (external instanceof JSMarkers) {
      switch ((JSMarkers)external) {
        case RETURN:
          if (subGens.size() == 1) {
            return subGens.get(0).Bind(new Function<AstNode, Generator>() {
              public Generator call(AstNode result) {
                return new ReturnGenerator(result);
              }
            });
          }
          break;
        case OBJECT:
          return Monad.SequenceBind(subGens,new JSGenFunction<List<AstNode>>() {
            public AstNode Generate(
                String in,
                String out,
                Function<AstNode, AstNode> returnFunction,
                List<AstNode> elements) {
              return JSUtil.genObject(elements);
            }
          });
      }
    } else if (external instanceof JSMarkerWithInfo) {
      JSMarkerWithInfo<?> markerWithInfo = (JSMarkerWithInfo<?>)external;
      switch ((JSMarkers)markerWithInfo.marker) {
        case PROPERTY:
          final String _propName = (String)markerWithInfo.info;
          if (subGens.size() == 1) {
            return subGens.get(0).Bind(new JSGenFunction<AstNode>() {
              public AstNode Generate(
                  String in,
                  String out,
                  Function<AstNode, AstNode> returnFunction,
                  AstNode value) {
                return JSUtil.genObjectProperty(_propName, value);
              }
            });
          }
          break;
      }
    }
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
  public Generator setExtra(Generator exp, Object extraKey, Object extraInfo) {
    if (extraKey instanceof JSMarkers && extraInfo == true) {
      switch ((JSMarkers)extraKey) {
        case RETURN:
          return exp.Bind(new Function<AstNode, Generator>() {
            public Generator call(AstNode result) {
              return new ReturnGenerator(result);
            }
          });
        case STATEMENT:
          return exp.Bind(new Function<AstNode, Generator>() {
            public Generator call(AstNode result) {
              return new SequenceGenerator(result);
            }
          });
        case NOT:
          Generator exprGen = getLeftGenOfTruthyTest(exp);
          if (exprGen == null) {
            return exp;
          }
          return exprGen.Bind(new JSGenFunction<AstNode>() {
            public AstNode Generate(
                String in,
                String out,
                Function<AstNode, AstNode> returnFunction,
                AstNode inner) {
              return JSUtil.genUnary(Token.NOT, inner);
            }
          });
        case AND:
          return getInfixBooleanOperator(Token.AND, exp, false);
        case OR:
          return getInfixBooleanOperator(Token.OR, exp, true);
      }
    }
    return exp.setExtra(extraKey, extraInfo);
  }

  private Generator getLeftGenOfTruthyTest(Generator gen) {
    List<Object> info = MarkedGenerator.getInfoFor(gen, JSMarkers.LET);
    return info == null
      ? null
      : (Generator)info.get(1);
  }

  private Generator getRightGenOfTruthyTest(
      Generator gen,
      boolean isThenBranch) {
    List<Object> letInfo = MarkedGenerator.getInfoFor(gen, JSMarkers.LET);
    if (letInfo != null) {
      Generator body = (Generator)letInfo.get(2);
      List<Object> ifInfo = MarkedGenerator.getInfoFor(body, JSMarkers.IF);
      if (ifInfo != null) {
        return (Generator)ifInfo.get(isThenBranch ? 1 : 2);
      }
    }
    return null;
  }

  private Generator getInfixBooleanOperator(
      final int _type,
      Generator gen,
      boolean isThenBranch) {
    Generator leftGen = getLeftGenOfTruthyTest(gen);
    Generator rightGen = getRightGenOfTruthyTest(gen, isThenBranch);
    if (leftGen == null || rightGen == null) {
      return gen;
    }
    return Monad.Bind2(leftGen, rightGen,
      new JSGenFunction2<AstNode, AstNode>() {
        public AstNode Generate(
            String in,
            String out,
            Function<AstNode, AstNode> returnFunction,
            AstNode left,
            AstNode right) {
          return JSUtil.genInfix(_type, left, right);
        }
      }
    );
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
