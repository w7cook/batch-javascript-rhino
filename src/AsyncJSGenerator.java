import org.mozilla.javascript.ast.AstNode;

public abstract class AsyncJSGenerator extends Generator {
  protected final Function<AstNode, Generator> callback;

  public AsyncJSGenerator(Function<AstNode, Generator> callback) {
    this.callback = callback;
  }

  public abstract AsyncJSGenerator cloneFor(
    Function<AstNode, Generator> callback
  );

  public AsyncJSGenerator Bind(
      final Function<AstNode, Generator> _nextCallback) {
    if (this.callback == null) {
      return this.cloneFor(_nextCallback);
    } else {
      final Function<AstNode, Generator> _currCallback = this.callback;
      return this.cloneFor(
        new Function<AstNode, Generator>() {
          public Generator call(AstNode node) {
            return _currCallback.call(node).Bind(_nextCallback);
          }
        }
      );
    }
  }
}
