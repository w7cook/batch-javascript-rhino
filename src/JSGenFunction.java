import org.mozilla.javascript.ast.AstNode;

abstract public class JSGenFunction<I> extends Function<I, Generator> {
  public Generator call(final I _param) {
    final JSGenFunction<I> _jsGenFunc = this;
    return new Generator() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction) {
        return _jsGenFunc.Generate(in, out, returnFunction, _param);
      }

      // TODO: Why is this different from other JSGenFunction#s
      public Generator Bind(Function<AstNode, Generator> f) {
        return _jsGenFunc.Bind(f, _param);
      }
    };
  }

  abstract public AstNode Generate(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction,
      I param
    );

  private Generator Bind(
      final Function<AstNode, Generator> _f,
      final I _param) {
    final JSGenFunction<I> _jsGenFunc = this;
    return (new Generator() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction) {
        return _jsGenFunc.Generate(in, out, returnFunction, _param);
      }
    }).Bind(_f);
  }
}
