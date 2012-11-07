import org.mozilla.javascript.ast.AstNode;

abstract public class JSGenFunction<I> extends Function<I, Generator> {
  public Generator call(final I _param) {
    final JSGenFunction<I> _jsGenFunc = this;
    return new Generator() {
      public AstNode Generate(String in, String out) {
        return _jsGenFunc.Generate(in, out, _param);
      }

      public Generator Bind(Function<AstNode, Generator> f) {
        return _jsGenFunc.Bind(f, _param);
      }
    };
  }

  abstract public AstNode Generate(String in, String out, I param);

  public Generator Bind(
      final Function<AstNode, Generator> _f,
      final I _param) {
    final JSGenFunction<I> _jsGenFunc = this;
    return (new Generator() {
      public AstNode Generate(String in, String out) {
        return _jsGenFunc.Generate(in, out, _param);
      }
    }).Bind(_f);
  }
}
