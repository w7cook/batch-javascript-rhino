import batch.partition.PartitionFactory;
import batch.syntax.Format;

import java.util.ArrayList;
import java.util.List;

class FormatPartition extends Format implements PartitionFactory<String> {

  public String Other(Object external, List<String> subs) {
    return noimpl();
  }

  public String DynamicCall(String target, String method,
      List<String> args) {
    return noimpl();
  }

  public String Mobile(String type, String exp) {
    return noimpl();
  }

  @Override
  public String Other(Object external, String... subs) {
    return noimpl();
  }

  @Override
  public String setExtra(String exp, Object extra) {
    return exp;
  }

  private <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }

}
