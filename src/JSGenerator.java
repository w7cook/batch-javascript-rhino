import org.mozilla.javascript.ast.AstNode;

import batch.partition.ExtraInfo;

public abstract class JSGenerator implements ExtraInfo<JSGenerator> {
  Object extraInfo;

  public JSGenerator setExtra(Object info) {
    extraInfo = info;
    return this;
  }

  abstract public AstNode generateNode(String in, String out);
  //abstract public AstNode generateRemote(String in, String out);
}
