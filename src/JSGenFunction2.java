import org.mozilla.javascript.ast.AstNode;

abstract public class JSGenFunction2<A,B>
    extends Function<Pair<A,B>, Generator> {
  public Generator call(final Pair<A,B> _params) {
    final JSGenFunction2<A,B> _jsGenFunc = this;
    return new Generator() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction) {
        return _jsGenFunc.Generate(
          in,
          out,
          returnFunction,
          _params.first,
          _params.second
        );
      }
    };
  }

  abstract public AstNode Generate(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction,
      A a,
      B b
    );
}
