import org.mozilla.javascript.ast.*;

import batch.partition.DynamicCallInfo;
import batch.partition.Place;

import java.util.List;
import java.util.Iterator;

public class DynamicCallGenerator extends AsyncJSGenerator {

  private static int index = 0;
  private static String genNextString() {
    index++;
    return "dc$"+index;
  }

  private final String function;
  private final List<AstNode> args;
  private final DynamicCallInfo callInfo;

  public DynamicCallGenerator(
      String function,
      List<AstNode> args,
      DynamicCallInfo callInfo) {
    this(function, args, callInfo, null);
  }

  public DynamicCallGenerator(
      String function,
      List<AstNode> args,
      DynamicCallInfo callInfo,
      Function<AstNode, Generator> callback) {
    super(callback);
    this.function = function;
    this.args = args;
    this.callInfo = callInfo;
  }

  public AstNode Generate(
      final String _in,
      final String _out,
      final Function<AstNode, AstNode> _returnFunction) {
    final DynamicCallGenerator _dcGen = this;
    return new FunctionCall() {{
      setTarget(JSUtil.genName(_dcGen.function+"$postLocal"));
      addArgument(JSUtil.genName(_in));
      // local args
      Iterator<AstNode> argIt = args.iterator();
      Iterator<Place> placeIt = callInfo.arguments.iterator();
      while (argIt.hasNext() && placeIt.hasNext()) {
        AstNode arg = argIt.next();
        if (placeIt.next() != Place.REMOTE) {
          addArgument(arg);
        }
      }
      addArgument(new FunctionNode() {{
        String param = _dcGen.genNextString();
        addParam(JSUtil.genName(param));
        setBody(
          _dcGen.callback != null
            ? JSUtil.genBlock(
                _dcGen
                  .callback
                  .call(JSUtil.genName(param))
                  .Generate(_in, _out, _returnFunction)
              )
            : new Block()
        );
      }});
    }};
  }

  public DynamicCallGenerator cloneFor(Function<AstNode, Generator> newCallback) {
    return new DynamicCallGenerator(function, args, callInfo, newCallback);
  }
}

