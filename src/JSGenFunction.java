import org.mozilla.javascript.ast.AstNode;

abstract public class JSGenFunction<I> extends Function<I, JSGenerator> {
  public JSGenerator call(final I _param) {
    final JSGenFunction<I> _jsGenFunc = this;
    return new JSGenerator() {
      public AstNode Generate(String in, String out) {
        return _jsGenFunc.Generate(in, out, _param);
      }
    };
  }

  abstract public AstNode Generate(String in, String out, I param);
}
