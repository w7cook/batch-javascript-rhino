import org.mozilla.javascript.ast.*;

import java.util.ArrayList;

public class LoopGenerator extends AsyncJSGenerator {

  private final String var;
  private final AstNode collection;
  private final Generator bodyGen;

  public LoopGenerator(String var, AstNode collection, Generator bodyGen) {
    this(var, collection, bodyGen, null);
  }

  public LoopGenerator(
      String var,
      AstNode collection,
      Generator bodyGen,
      Function<AstNode, Generator> callback) {
    super(callback);
    this.var = var;
    this.collection = collection;
    this.bodyGen = bodyGen;
  }

  public AstNode Generate(final String _in, final String _out) {
    final LoopGenerator _loopGen = this;
    if (!(collection instanceof JSPartitionFactory.EmptyNode)) {
      return noimpl();
    }
    final String _next = var+"_next";
    return JSUtil.genCall(
      JSUtil.genName(_in),
      "asyncForEach",
      new ArrayList<AstNode>() {{
        add(JSUtil.genStringLiteral(_loopGen.var));
        add(new FunctionNode() {{
          addParam(JSUtil.genName(_loopGen.var));
          addParam(JSUtil.genName(_next));
          setBody(
            _loopGen.bodyGen.Bind(new JSGenFunction<AstNode>() {
              public AstNode Generate(
                  String in,
                  String out,
                  final AstNode _body) {
                return new Block() {{
                  addStatement(JSUtil.genStatement(_body));
                  // TODO: fold down one
                  addStatement(JSUtil.genStatement(
                    new FunctionCall() {{
                      setTarget(JSUtil.genName(_next));
                    }}
                  ));
                }};
              }
            }).Generate(
              _in  != null ? _loopGen.var : null,
              _out != null ? _loopGen.var : null
            )
          );
        }});
        add(new FunctionNode() {{
          setBody(
            _loopGen.callback != null
              ? _loopGen.callback.call(new EmptyExpression())
                  .Generate(_in, _out)
              : new EmptyStatement()
          );
        }});
      }}
    );
  }

  public LoopGenerator cloneFor(Function<AstNode, Generator> newCallback) {
    return new LoopGenerator(
      this.var,
      this.collection,
      this.bodyGen,
      newCallback
    );
  }

  private static <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}

