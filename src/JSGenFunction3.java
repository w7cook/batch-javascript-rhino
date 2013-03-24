import org.mozilla.javascript.ast.AstNode;

abstract public class JSGenFunction3<A,B,C>
    extends Function<Pair<A,Pair<B,C>>, Generator> {
  public Generator call(final Pair<A,Pair<B,C>> _params) {
    final JSGenFunction3<A,B,C> _jsGenFunc = this;
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
          _params.second.first,
          _params.second.second
        );
      }
    };
  }

  abstract public AstNode Generate(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction,
      A a,
      B b,
      C c
    );
}
