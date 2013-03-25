import org.mozilla.javascript.ast.AstNode;

public class SequenceGenerator extends CallbackManipulatorGenerator {
  private AstNode statements;
  private AstNode value;

  public SequenceGenerator() {
    this(null);
  }

  public SequenceGenerator(AstNode statements) {
    this(statements, null); // TODO maybe: mark not value
  }

  public SequenceGenerator(AstNode statements, AstNode value) {
    this.statements = statements;
    this.value = value;
  }

  @Override
  public AstNode GenerateOn(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction,
      Function<AstNode, Generator> callback) {
    return JSUtil.concatBlocks(
      statements,
      callback == null
        ? value
        : callback.call(value).Generate(in, out, returnFunction)
    );
  }
}
