import org.mozilla.javascript.ast.AstNode;

import java.util.Arrays;
import java.util.List;

public class MarkedGenerator extends Generator {
  private final Generator gen;
  public final JSMarkers marker;
  public final List<Object> info;

  public MarkedGenerator(Generator gen, JSMarkers marker, Object... info) {
    this.gen = gen;
    this.marker = marker;
    this.info = Arrays.asList(info);
  }

  public AstNode Generate(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction
    ) {
    return gen.Generate(in, out, returnFunction);
  }

  public Generator Bind(Function<AstNode, Generator> f) {
    return gen.Bind(f);
  }

  public Generator setExtra(Object key, Object info) {
    gen.setExtra(key, info);
    return this;
  }

  public static List<Object> getInfoFor(Generator gen, JSMarkers marker) {
    if (gen instanceof MarkedGenerator) {
      MarkedGenerator markedGen = (MarkedGenerator)gen;
      if (markedGen.marker == marker) {
        return (List<Object>)markedGen.info;
      }
    }
    return null;
  }
}
