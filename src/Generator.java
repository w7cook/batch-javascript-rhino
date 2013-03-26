import org.mozilla.javascript.ast.AstNode;

import batch.partition.ExtraInfo;

import java.util.HashMap;
import java.util.Map;

abstract public class Generator implements ExtraInfo<Generator> {
  protected Map<Object,Object> extras = new HashMap<Object,Object>();

  public Generator setExtra(Object key, Object info) {
    extras.put(key, info);
    return this;
  }

  abstract public AstNode Generate(
      String remoteIn,
      String remoteOut,
      Function<AstNode, AstNode> returnFunction
    );

  public static Generator Return(final AstNode _r) {
    return new Generator() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction) {
        return _r;
      }
    };
  }

  public Generator Bind(final Function<AstNode, Generator> _f) {
    final Generator _param = this;
    return new Generator() {
      public AstNode Generate(
          String in,
          String out,
          Function<AstNode, AstNode> returnFunction) {
        return _f.call(_param.Generate(in, out, returnFunction))
                 .Generate(in, out, returnFunction);
      }
      public Generator Bind(final Function<AstNode, Generator> _g) {
        return _param.Bind(new Function<AstNode, Generator>() {
          public Generator call(AstNode node) {
            return _f.call(node).Bind(_g);
          }
        });
      }
    };
  }
}

