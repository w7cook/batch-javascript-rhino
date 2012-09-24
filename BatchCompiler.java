// WHAT IS: f.Mobile(...),
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import batch.Op;
import batch.syntax.Format;
import batch.partition.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class BatchCompiler implements NodeVisitor {
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
      // TODO: replace code
      //System.out.println(compiler.compiledBatchNode.toSource());
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
    Name rootName = null;
    switch (batch.getIterator().getType()) {
      case Token.VAR:
        rootName = (Name)((VariableDeclaration)batch.getIterator())
          .getVariables().get(0).getTarget();
        break;
      case Token.NAME:
        rootName = (Name)batch.getIterator();
        break;
      default:
        noimpl();
    }
    CodeModel.factory.allowAllTransers = true;
    PExpr origExpr = new JSToPartition(rootName.getIdentifier())
      .exprFrom(CodeModel.factory, batch.getBody());
    Environment env = new Environment(CodeModel.factory)
      .extend(CodeModel.factory.RootName(), null, Place.REMOTE);
    History history = origExpr.partition(Place.MOBILE, env);
    for (Stage stage : history) {
      switch (stage.place()) {
        case LOCAL:
          AstNode local = stage.action().runExtra(new JSPartitionFactory());
          System.out.println("Local:\n"+local.toSource());
          break;
        case REMOTE:
          System.out.println("Remote:\n"+stage.action().runExtra(new FormatPartition()));
          //script = '...';
          //var forest = BatchServer.send(script);
          //for (var i=0; i<forest.length; i++) {
          //  var INPUT = forest[i];
          //  console.log(INPUT.g0);
          //}
          break;
      }
    }
    compiledBatchNode = batch;
    return false;
  }

  private <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
