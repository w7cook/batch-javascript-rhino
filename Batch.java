import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

import java.util.Scanner;

class Batch {
  public static void main(String[] args) {
    String fileName = args[0];
    Scanner scanner = new Scanner(new File(fileName));
    AstRoot ast = Parser.parse(source, fileName, /*linenumber*/ 0);
  }
}
