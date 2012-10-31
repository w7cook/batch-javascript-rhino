import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;

import java.util.ArrayList;

public class InGenerator extends Generator {

  private static int index = 0;
  private static String genNextString() {
    index++;
    return "g$"+index;
  }

  private String location;
  private Function<AstNode, Generator> callback;

  public InGenerator(String location) {
    this(location, null);
  }

  public InGenerator(
      String location,
      Function<AstNode, Generator> callback) {
    this.location = location;
    this.callback = callback;
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
          if (_inGen.callback != null) {
            setBody(
              _inGen.callback.call(JSUtil.genName(param))
                .Generate(_in, _out)
            );
          }
        }});
      }}
    );
  }

  public InGenerator Bind(final Function<AstNode, Generator> _nextCallback) {
    if (this.callback == null) {
      return new InGenerator(location, _nextCallback);
    } else {
      final Function<AstNode, Generator> _currCallback = this.callback;
      return new InGenerator(
        location,
        new Function<AstNode, Generator>() {
          public Generator call(AstNode node) {
            return _currCallback.call(node).Bind(_nextCallback);
          }
        }
      );
    }
  }
}

