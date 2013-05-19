import org.mozilla.javascript.ast.*;

import java.util.ArrayList;

// paper TODO: Work with terinary conditional operator
public class IfGenerator extends CallbackManipulatorGenerator {

  private static int index = 0;
  private static String genNextString() {
    index++;
    return "if_callback$"+index;
  }

  private final Generator conditionGen;
  private final Generator thenExprGen;
  private final Generator elseExprGen;

  public IfGenerator(
      Generator conditionGen,
      Generator thenExprGen,
      Generator elseExprGen) {
    this.conditionGen = conditionGen;
    this.thenExprGen = thenExprGen;
    this.elseExprGen = elseExprGen;
  }

  @Override
  public AstNode GenerateOn(
      final String _in,
      final String _out,
      final Function<AstNode, AstNode> _returnFunction,
      final Function<AstNode, Generator> _callback) {
    final IfGenerator _ifGen = this;
    return conditionGen.Bind(new JSGenFunction<AstNode>() {
      public AstNode Generate(
          final String _in,
          final String _out,
          final Function<AstNode, AstNode> _returnFunction,
          final AstNode _condition) {
        final String _callbackName = _ifGen.genNextString();
        final Function<AstNode, Generator> _postBranch =
          new JSGenFunction<AstNode>() {
            public AstNode Generate(
                String in,
                String out,
                Function<AstNode, AstNode> returnFunction,
                final AstNode _result) {
              final AstNode _valueResult =
                JSUtil.isEmpty(_result)
                  ? JSUtil.genUndefined()
                  : _result;
              return JSUtil.genCall(
                JSUtil.genName(_callbackName),
                _valueResult
              );
            }
          };
        return new FunctionCall() {{
          setTarget(new ParenthesizedExpression(new FunctionNode() {{
            addParam(JSUtil.genName(_callbackName));
            setBody(JSUtil.genBlock(new IfStatement() {{
              setCondition(_condition);
              setThenPart(JSUtil.genBlock(
                _ifGen.thenExprGen
                  .Bind(_postBranch)
                  .Generate(_in, _out, _returnFunction)
              ));
              setElsePart(JSUtil.genBlock(
                _ifGen.elseExprGen
                  .Bind(_postBranch)
                  .Generate(_in, _out, _returnFunction)
              ));
            }}));
          }}));
          addArgument(new FunctionNode() {{
            addParam(JSUtil.genName("value$"));
            setBody(
              _callback != null
                ? JSUtil.genBlock(
                    _callback
                      .call(
                        // only pass value if this is not a statement
                        _ifGen.extras.get(JSMarkers.IF_STATEMENT) == true
                          ? null
                          : JSUtil.genName("value$")
                      )
                      .Generate(_in, _out, _returnFunction)
                  )
                : JSUtil.concatBlocks()
            );
          }});
        }};
      }
    }).Generate(_in, _out, _returnFunction); // generate bounded condition
  }
}


