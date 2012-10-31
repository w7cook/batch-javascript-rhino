import org.mozilla.javascript.ast.AstNode;

abstract public class JSGenFunction<I> extends Function<I, Generator<AstNode>> {
  public Generator<AstNode> call(final I _param) {
    final JSGenFunction<I> _jsGenFunc = this;
    return new Generator<AstNode>() {
      public AstNode Generate(String in, String out) {
        return _jsGenFunc.Generate(in, out, _param);
      }
    };
  }

  abstract public AstNode Generate(String in, String out, I param);
}
