import org.mozilla.javascript.ast.AstNode;

import batch.partition.ExtraInfo;

abstract public class JSGenerator
    implements ExtraInfo<JSGenerator>, MonadI<JSGenerator, AstNode> {
  Object extraInfo;

  public JSGenerator setExtra(Object info) {
    extraInfo = info;
    return this;
  }

  abstract public AstNode Generate(String remoteIn, String remoteOut);

  public static JSGenerator Return(final AstNode _node) {
    return new JSGenerator() {
      public AstNode Generate(String in, String out) {
        return _node;
      }
    };
  }

  @Override
  public <R> MonadI<JSGenerator, R> Bind(
      final Function<AstNode, ? extends MonadI<JSGenerator, R>> f) {
    return noimpl();
    //final JSGenerator _param = this;
    //return new JSGenerator() {
    //  public AstNode Generate(String in, String out) {
    //    return unboundJSGen.call(_param.Generate(in, out)).Generate(in, out);
    //  }
    //};
  }

  private static <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
