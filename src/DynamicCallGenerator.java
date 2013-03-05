import org.mozilla.javascript.ast.*;

import java.util.List;

public class DynamicCallGenerator extends AsyncJSGenerator {

  private static int index = 0;
  private static String genNextString() {
    index++;
    return "dc$"+index;
  }

  private final String method;
  private final List<AstNode> args;

  public DynamicCallGenerator(String method, List<AstNode> args) {
    this(method, args, null);
  }

  public DynamicCallGenerator(
      String method,
      List<AstNode> args,
      Function<AstNode, Generator> callback) {
    super(callback);
    this.method = method;
    this.args = args;
  }

  public AstNode Generate(final String _in, final String _out) {
    final DynamicCallGenerator _dcGen = this;
    return new FunctionCall() {{
      setTarget(JSUtil.genName(_dcGen.method+"$postLocal"));
      addArgument(JSUtil.genName(_in));
      addArgument(new FunctionNode() {{
        String param = _dcGen.genNextString();
        // TODO: local args
        addParam(JSUtil.genName(param));
        setBody(
          _dcGen.callback != null
            ? JSUtil.genBlock(
                _dcGen
                  .callback
                  .call(JSUtil.genName(param))
                  .Generate(_in, _out)
              )
            : new Block()
        );
      }});
    }};
  }

  public DynamicCallGenerator cloneFor(Function<AstNode, Generator> newCallback) {
    return new DynamicCallGenerator(method, args, newCallback);
  }
}

