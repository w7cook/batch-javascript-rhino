import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.partition.*;

import java.util.List;
import java.util.Map;

public class LocalPartitionToJS extends PartitionFactoryHelper<Generator> {

  private Map<String, DynamicCallInfo> batchFunctionsInfo;
  private static final RawJSFactory rawJSFactory = new RawJSFactory();

  public LocalPartitionToJS(Map<String, DynamicCallInfo> batchFunctionsInfo) {
    this.batchFunctionsInfo = batchFunctionsInfo;
  }

  @Override
  public Generator Fun(final String _var, final Generator _body) {
    return new Generator() {
      public AstNode Generate(
          final String _in,
          final String _out,
          final Function<AstNode, AstNode> _returnFunction) {
        // paper TODO: Prevent remote code in body
        // TODO:
        //  x.set_f(function (x) {
        //    return x + name; // not converting to INPUT
        //  })
        //  x.f("*")
        return new FunctionNode() {{
          addParam(JSUtil.genName(_var));
          setBody(JSUtil.genBlock(_body.Generate(_in, _out, /*TODO*/ JSUtil.<Function<AstNode,AstNode>>noimpl())));
        }};
      }
    };
  }

  @Override
  public Generator If(
      Generator conditionGen,
      Generator thenExprGen,
      Generator elseExprGen) {
    return new MarkedGenerator(
      new IfGenerator(conditionGen, thenExprGen, elseExprGen),
      JSMarkers.IF,
      conditionGen,
      thenExprGen,
      elseExprGen
    );
  }

  @Override
  public Generator Loop(
      String var,
      Generator collectionGen,
      Generator bodyGen) {
    return new LoopGenerator(var, bodyGen);
  }

  @Override
  public Generator In(String location) {
    return new InGenerator(location);
  }

  @Override
  public Generator Out(final String _location, Generator expressionGen) {
    return expressionGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction,
          AstNode expression) {
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
  public Generator DynamicCall(
      Generator targetGen,
      final String _method,
      final List<Generator> _argGens) {
    return targetGen.Bind(new Function<AstNode,Generator>() {
      public Generator call(AstNode target) {
        if (JSUtil.isEmpty(target)) {
          return Monad.SequenceBind(
            _argGens,
            new Function<List<AstNode>, Generator>() {
              public Generator call(List<AstNode> args) {
                return new DynamicCallGenerator(
                  _method,
                  args,
                  batchFunctionsInfo.get(_method)
                );
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
  public Generator Var(String name) {
    return rawJSFactory.Var(name);
  }

  @Override
  public Generator Data(Object value) {
    return rawJSFactory.Data(value);
  }

  @Override
  public Generator Prim(Op op, List<Generator> argGens) {
    return rawJSFactory.Prim(op, argGens);
  }

  @Override
  public Generator Prop(Generator baseGen, String field) {
    return rawJSFactory.Prop(baseGen, field);
  }

  @Override
  public Generator Assign(Generator targetGen, Generator sourceGen) {
    return rawJSFactory.Assign(targetGen, sourceGen);
  }

  @Override
  public Generator Let(String var, Generator expressionGen, Generator bodyGen) {
    return rawJSFactory.Let(var, expressionGen, bodyGen);
  }

  @Override
  public Generator Call(
      Generator targetGen,
      String method,
      List<Generator> argGens) {
    return rawJSFactory.Call(targetGen, method, argGens);
  }

  @Override
  public Generator Other(Object external, List<Generator> subGens) {
    return rawJSFactory.Other(external, subGens);
  }

  @Override
  public Generator Mobile(String type, Generator exp) {
    return rawJSFactory.Mobile(type, exp);
  }

  @Override
  public Generator setExtra(Generator exp, Object extraKey, Object extraInfo) {
    return rawJSFactory.setExtra(exp, extraKey, extraInfo);
  }

  @Override
  public Generator Root() {
    return rawJSFactory.Root();
  }

  @Override
  public Generator Skip() {
    return rawJSFactory.Skip();
  }
}
