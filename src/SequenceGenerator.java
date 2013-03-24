import org.mozilla.javascript.ast.AstNode;

public class SequenceGenerator extends AsyncJSGenerator {
  private AstNode statements;
  private AstNode value;

  public SequenceGenerator() {
    this(null);
  }

  public SequenceGenerator(AstNode statements) {
    this(statements, null); // TODO maybe: mark not value
  }

  public SequenceGenerator(AstNode statements, AstNode value) {
    this(statements, value, null);
  }

  public SequenceGenerator(
      AstNode statements,
      AstNode value,
      Function<AstNode, Generator> callback) {
    super(callback);
    this.statements = statements;
    this.value = value;
  }

  public AstNode Generate(
      String in,
      String out,
      Function<AstNode, AstNode> returnFunction) {
    return JSUtil.concatBlocks(
      statements,
      callback == null
        ? value
        : callback.call(value).Generate(in, out, returnFunction)
    );
  }

  public SequenceGenerator cloneFor(Function<AstNode, Generator> newCallback) {
    return new SequenceGenerator(statements, value, newCallback);
  }
}
