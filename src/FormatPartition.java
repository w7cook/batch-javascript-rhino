import batch.partition.PartitionFactory;
import batch.syntax.Format;

import java.util.ArrayList;
import java.util.List;

class FormatPartition extends Format implements PartitionFactory<String> {

  @Override
  public String Other(Object external, List<String> subs) {
    return JSUtil.noimpl();
  }

  @Override
  public String DynamicCall(String target, String method,
      List<String> args) {
    return JSUtil.noimpl();
  }

  @Override
  public String Mobile(String type, String exp) {
    return JSUtil.noimpl();
  }

  @Override
  public String Other(Object external, String... subs) {
    return JSUtil.noimpl();
  }

  @Override
  public String setExtra(String exp, Object extra) {
    return exp;
  }

}
