import org.mozilla.javascript.ast.*;

import batch.partition.DynamicCallInfo;
import batch.partition.Place;

import java.util.List;
import java.util.Iterator;

public class DynamicCallGenerator extends CallbackManipulatorGenerator {

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
    this.function = function;
    this.args = args;
    this.callInfo = callInfo;
  }

  @Override
  public AstNode GenerateOn(
      final String _in,
      final String _out,
      final Function<AstNode, AstNode> _returnFunction,
      final Function<AstNode, Generator> _callback) {
    if (callInfo.returns == Place.REMOTE) {
      return _callback.call(null).Generate(_in, _out, _returnFunction);
    } else {
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
            _callback != null
              ? JSUtil.genBlock(
                  _callback
                    .call(JSUtil.genName(param))
                    .Generate(_in, _out, _returnFunction)
                )
              : new Block()
          );
        }});
      }};
    }
  }
}

