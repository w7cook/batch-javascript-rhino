import org.mozilla.javascript.ast.AstNode;

abstract public class JSGenFunction2<A,B>
    extends Function<Pair<A,B>, JSGenerator> {
  public JSGenerator call(final Pair<A,B> _params) {
    final JSGenFunction2<A,B> _jsGenFunc = this;
    return new JSGenerator() {
      public AstNode Generate(String in, String out) {
        return _jsGenFunc.Generate(in, out, _params.first, _params.second);
      }
    };
  }

  abstract public AstNode Generate(String in, String out, A a, B b);
}
