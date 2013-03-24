import org.mozilla.javascript.ast.AstNode;

public class ReturnGenerator extends Generator {

  private AstNode result;

  public ReturnGenerator(AstNode result) {
    this.result = result;
  }

  public AstNode Generate(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction) {
    return returnFunction.call(result);
  }

  public Generator Bind(Function<AstNode, Generator> f) {
    return this;
  }
}
