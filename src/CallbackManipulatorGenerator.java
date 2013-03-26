import org.mozilla.javascript.ast.AstNode;

public abstract class CallbackManipulatorGenerator extends Generator {
  public abstract AstNode GenerateOn(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction,
      Function<AstNode, Generator> callback
    );
  
  public AstNode Generate(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction) {
    return GenerateOn(in, out, returnFunction, null);
  }

  public Generator Bind(Function<AstNode, Generator> callback) {
    return new CallbackAccumulatingGenerator(callback);
  }

  private class CallbackAccumulatingGenerator extends Generator {
    private Function<AstNode, Generator> callback;
    public CallbackAccumulatingGenerator(Function<AstNode, Generator> callback) 
    {
      this.callback = callback;
    }
    public AstNode Generate(
        String in,
        String out,
        Function<AstNode, AstNode> returnFunction) {
      return
        CallbackManipulatorGenerator
          .this
          .GenerateOn(in, out, returnFunction, callback);
    }
    public Generator Bind(final Function<AstNode, Generator> _nextCallback) {
      final CallbackAccumulatingGenerator _this = this;
      return new CallbackAccumulatingGenerator(
        new Function<AstNode, Generator>() {
          public Generator call(AstNode result) {
            return _this.callback.call(result).Bind(_nextCallback);
          }
        }
      );
    }
  }
}
