import org.mozilla.javascript.ast.*;

import java.util.ArrayList;

// paper TODO: Work with terinary conditional operator
public class IfGenerator extends AsyncJSGenerator {

  private static int index = 0;
  private static String genNextString() {
    index++;
    return "if_callback$"+index;
  }

  private final AstNode condition;
  private final Generator thenExprGen;
  private final Generator elseExprGen;

  public IfGenerator(
      AstNode condition,
      Generator thenExprGen,
      Generator elseExprGen) {
    this(condition, thenExprGen, elseExprGen, null);
  }

  public IfGenerator(
      AstNode condition,
      Generator thenExprGen,
      Generator elseExprGen,
      Function<AstNode, Generator> callback) {
    super(callback);
    this.condition = condition;
    this.thenExprGen = thenExprGen;
    this.elseExprGen = elseExprGen;
  }

  public AstNode Generate(final String _in, final String _out) {
    final IfGenerator _ifGen = this;
    return new FunctionCall() {{
      setTarget(new ParenthesizedExpression(new FunctionNode() {{
        final String _callback = _ifGen.genNextString();
        addParam(JSUtil.genName(_callback));
        setBody(JSUtil.genBlock(new IfStatement() {{
          setCondition(_ifGen.condition);
          setThenPart(JSUtil.genBlock(
            _ifGen.thenExprGen.Bind(new JSGenFunction<AstNode>() {
              public AstNode Generate(
                  String in,
                  String out,
                  final AstNode _then) {
                return JSUtil.concatBlocks(
                  _then,
                  new FunctionCall() {{
                    setTarget(JSUtil.genName(_callback));
                  }}
                );
              }
            }).Generate(_in, _out)
          ));
          setElsePart(JSUtil.genBlock(
            _ifGen.elseExprGen.Bind(new JSGenFunction<AstNode>() {
              public AstNode Generate(
                  String in,
                  String out,
                  final AstNode _else) {
                return JSUtil.concatBlocks(
                  _else,
                  new FunctionCall() {{
                    setTarget(JSUtil.genName(_callback));
                  }}
                );
              }
            }).Generate(_in, _out)
          ));
        }}));
      }}));
      addArgument(new FunctionNode() {{
        setBody(
          _ifGen.callback != null
            ? JSUtil.genBlock(
                _ifGen.callback.call(new EmptyExpression())
                  .Generate(_in, _out)
              )
            : new Block()
        );
      }});
    }};
  }

  public IfGenerator cloneFor(Function<AstNode, Generator> newCallback) {
    return new IfGenerator(condition, thenExprGen, elseExprGen, newCallback);
  }
}


