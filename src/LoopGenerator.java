import org.mozilla.javascript.ast.*;

import java.util.ArrayList;

public class LoopGenerator extends AsyncJSGenerator {

  private final String var;
  private final Generator bodyGen;

  public LoopGenerator(String var, Generator bodyGen) {
    this(var, bodyGen, null);
  }

  public LoopGenerator(
      String var,
      Generator bodyGen,
      Function<AstNode, Generator> callback) {
    super(callback);
    this.var = var;
    this.bodyGen = bodyGen;
  }

  public AstNode Generate(
      final String _in,
      final String _out,
      final Function<AstNode, AstNode> _returnFunction) {
    final LoopGenerator _loopGen = this;
    final String _next = var+"_next";
    return JSUtil.genCall(
      JSUtil.genName(_in),
      "asyncForEach",
      new ArrayList<AstNode>() {{
        add(JSUtil.genStringLiteral(_loopGen.var));
        add(new FunctionNode() {{
          addParam(JSUtil.genName(_loopGen.var));
          addParam(JSUtil.genName(_next));
          setBody(
            JSUtil.genBlock(
              _loopGen.bodyGen.Bind(new JSGenFunction<AstNode>() {
                public AstNode Generate(
                    String in,
                    String out,
                    Function<AstNode, AstNode> returnFunction,
                    AstNode _) {
                  return JSUtil.genCall(
                    JSUtil.genName(_next),
                    JSUtil.genFalse(),
                    JSUtil.genUndefined()
                  );
                }
              }).Generate(
                _in  != null ? _loopGen.var : null,
                _out != null ? _loopGen.var : null,
                new Function<AstNode, AstNode>() {
                  public AstNode call(AstNode result) {
                    return JSUtil.genCall(
                      JSUtil.genName(_next),
                      JSUtil.genTrue(),
                      result
                    );
                  }
                }
              )
            )
          );
        }});
        add(new FunctionNode() {{
          addParam(JSUtil.genName("isReturning$"));
          addParam(JSUtil.genName("value$"));
          setBody(
            JSUtil.genBlock(
              new IfStatement() {{
                setCondition(JSUtil.genName("isReturning$"));
                setThenPart(
                  JSUtil.genBlock(
                  _returnFunction.call(JSUtil.genName("value$"))
                  )
                );
                setElsePart(
                  _loopGen.callback != null
                    ? JSUtil.genBlock(
                        _loopGen.callback.call(null)
                          .Generate(_in, _out, _returnFunction)
                      )
                    : new Block()
                );
              }}
            )
          );
        }});
      }}
    );
  }

  public LoopGenerator cloneFor(Function<AstNode, Generator> newCallback) {
    return new LoopGenerator(
      this.var,
      this.bodyGen,
      newCallback
    );
  }
}

