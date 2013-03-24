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

  public AstNode Generate(
      final String _in,
      final String _out,
      final Function<AstNode, AstNode> _returnFunction) {
    final IfGenerator _ifGen = this;
    final String _callback = _ifGen.genNextString();
    final Function<AstNode, Generator> _postBranch =
      new JSGenFunction<AstNode>() {
        public AstNode Generate(
            String in,
            String out,
            Function<AstNode, AstNode> returnFunction,
            final AstNode _result) {
          return JSUtil.genCall(
            JSUtil.genName(_callback),
            JSUtil.genFalse(),
            JSUtil.isEmpty(_result)
              ? JSUtil.genUndefined()
              : _result
          );
        }
      };
    final Function<AstNode, AstNode> _returnCallback =
      new Function<AstNode, AstNode>() {
        public AstNode call(AstNode result) {
          return JSUtil.genCall(
            JSUtil.genName(_callback), 
            JSUtil.genTrue(),
            result
          );
        }
      };
    return new FunctionCall() {{
      setTarget(new ParenthesizedExpression(new FunctionNode() {{
        addParam(JSUtil.genName(_callback));
        setBody(JSUtil.genBlock(new IfStatement() {{
          setCondition(_ifGen.condition);
          setThenPart(JSUtil.genBlock(
            _ifGen.thenExprGen
              .Bind(_postBranch)
              .Generate(_in, _out, _returnCallback)
          ));
          setElsePart(JSUtil.genBlock(
            _ifGen.elseExprGen
              .Bind(_postBranch)
              .Generate(_in, _out, _returnCallback)
          ));
        }}));
      }}));
      addArgument(new FunctionNode() {{
        addParam(JSUtil.genName("isReturning$")); // TODO: avoid collisions
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
              if (_ifGen.callback != null) {
                setElsePart(
                  JSUtil.genBlock(
                    _ifGen.callback.call(JSUtil.genName("value$"))
                      .Generate(_in, _out, _returnFunction)
                  )
                );
              }
            }}
          )
        );
      }});
    }};
  }

  public IfGenerator cloneFor(Function<AstNode, Generator> newCallback) {
    return new IfGenerator(condition, thenExprGen, elseExprGen, newCallback);
  }
}


