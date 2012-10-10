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

import java.util.ArrayList;

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
    compiledBatchNode = new Scope();
    compiledBatchNode.addChild(new ExpressionStatement(JSUtil.genDeclare(
      "s$",
      new NewExpression() {{
        setTarget(JSUtil.genName("Forest"));
      }}
    )));

    BatchLoop batch = (BatchLoop)node;
    String root = null;
    switch (batch.getIterator().getType()) {
      case Token.VAR:
        root =
          ((Name)((VariableDeclaration)batch.getIterator())
            .getVariables().get(0).getTarget()
          ).getIdentifier();
        break;
      case Token.NAME:
        root = ((Name)batch.getIterator()).getIdentifier();
        break;
      default:
        noimpl();
    }
    String service = null;
    switch (batch.getIteratedObject().getType()) {
      case Token.NAME:
        service = ((Name)batch.getIteratedObject()).getIdentifier();
        break;
      default:
        noimpl();
    }
    CodeModel.factory.allowAllTransers = true;
    PExpr origExpr =
      new JSToPartition<PExpr>(CodeModel.factory, root)
        .exprFrom(batch.getBody());
    Environment env = new Environment(CodeModel.factory)
      .extend(CodeModel.factory.RootName(), null, Place.REMOTE);
    History history = origExpr.partition(Place.MOBILE, env);
    for (Stage stage : history) {
      switch (stage.place()) {
        case LOCAL:
          AstNode local = stage
            .action()
            .runExtra(new JSPartitionFactory())
            .generateNode("r$", "s$");
          compiledBatchNode.addChild(local);
          break;
        case REMOTE:
          String script = stage.action().runExtra(new FormatPartition());
          compiledBatchNode.addChild(new ExpressionStatement(JSUtil.genDeclare(
            "script$",
            JSUtil.genStringLiteral(script)
          )));
          compiledBatchNode.addChild(new ExpressionStatement(JSUtil.genDeclare(
            "r$",
            JSUtil.genCall(
              JSUtil.genName(service),
              "execute",
              new ArrayList<AstNode>() {{
                add(JSUtil.genName("script$"));
                add(JSUtil.genName("s$"));
              }}
            )
          )));
          break;
      }
    }
    return false;
  }

  private <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
