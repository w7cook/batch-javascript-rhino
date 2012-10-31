import org.mozilla.javascript.ast.AstNode;

abstract public class JSGenFunction3<A,B,C>
    extends Function<Pair<A,Pair<B,C>>, JSGenerator> {
  public JSGenerator call(final Pair<A,Pair<B,C>> _params) {
    final JSGenFunction3<A,B,C> _jsGenFunc = this;
    return new JSGenerator() {
      public AstNode Generate(String in, String out) {
        return _jsGenFunc.Generate(
          in,
          out,
          _params.first,
          _params.second.first,
          _params.second.second
        );
      }
    };
  }

  abstract public AstNode Generate(String in, String out, A a, B b, C c);
}
