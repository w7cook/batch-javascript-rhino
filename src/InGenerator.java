import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.FunctionNode;

import java.util.ArrayList;

public class InGenerator extends AsyncJSGenerator {

  private static int index = 0;
  private static String genNextString() {
    index++;
    return "g$"+index;
  }

  private final String location;

  public InGenerator(String location) {
    this(location, null);
  }

  public InGenerator(
      String location,
      Function<AstNode, Generator> callback) {
    super(callback);
    this.location = location;
  }

  public AstNode Generate(final String _in, final String _out) {
    final InGenerator _inGen = this;
    return JSUtil.genCall(
      JSUtil.genName(_in),
      "get",
      new ArrayList<AstNode>() {{
        add(JSUtil.genStringLiteral(_inGen.location));
        add(new FunctionNode() {{
          String param = _inGen.genNextString();
          addParam(JSUtil.genName(param));
          setBody(
            _inGen.callback != null
              ? _inGen.callback.call(JSUtil.genName(param))
                  .Generate(_in, _out)
              : new EmptyStatement()
          );
        }});
      }}
    );
  }

  public InGenerator cloneFor(Function<AstNode, Generator> newCallback) {
    return new InGenerator(location, newCallback);
  }
}

