import org.mozilla.javascript.ast.*;

import java.util.ArrayList;

public class LoopGenerator extends CallbackManipulatorGenerator {

  private final String var;
  private final Generator bodyGen;

  public LoopGenerator(String var, Generator bodyGen) {
    this.var = var;
    this.bodyGen = bodyGen;
  }

  @Override
  public AstNode GenerateOn(
      final String _in,
      final String _out,
      final Function<AstNode, AstNode> _returnFunction,
      final Function<AstNode, Generator> _callback) {
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
                  return JSUtil.genCall(JSUtil.genName(_next));
                }
              }).Generate(
                _in  != null ? _loopGen.var : null,
                _out != null ? _loopGen.var : null,
                _returnFunction
              )
            )
          );
        }});
        add(new FunctionNode() {{
          setBody(
            _callback != null
              ? JSUtil.genBlock(
                  _callback
                    .call(null)
                    .Generate(_in, _out, _returnFunction)
                )
              : JSUtil.concatBlocks()
          );
        }});
      }}
    );
  }
}

