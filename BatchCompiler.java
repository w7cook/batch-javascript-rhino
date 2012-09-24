import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

class BatchCompiler implements NodeVisitor {
  public static void main(String[] args)
      throws FileNotFoundException, IOException {
    String fileName = args[0];
    FileReader reader = new FileReader(new File(fileName));
    Parser parser = new Parser();
      //new Parser(new CompilerEnvirons(), DefaultErrorReporter.instance);
    AstRoot ast = parser.parse(reader, fileName, /*linenumber*/ 0);
    BatchCompiler compiler = new BatchCompiler();
    ast.visit(compiler);
    if (compiler.compiledBatchNode != null) {
      System.out.println(compiler.compiledBatchNode.toSource());
    } else {
      System.out.println("No batch statement found");
    }
  }

  public AstNode compiledBatchNode;

  public boolean visit(AstNode node) {
    if (!(node instanceof BatchLoop)) {
      return true;
    }
    BatchLoop batch = (BatchLoop)node;
    compiledBatchNode = batch;
    return false;
  }
}
