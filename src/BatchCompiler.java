// TODO: package ...

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
import java.io.StringWriter;

import java.util.ArrayList;

public class BatchCompiler implements NodeVisitor {
  public static void main(String[] args)
      throws FileNotFoundException, IOException {
    String fileName = args[0];
    FileReader reader = new FileReader(new File(fileName));
    Parser parser = new Parser();
    // TODO Future: only parse batch code
    AstRoot ast = parser.parse(reader, fileName, /*linenumber*/ 0);
    BatchCompiler compiler = new BatchCompiler();
    ast.visit(compiler);
    if (compiler.compiledBatchNode != null) {
      // TODO Future: don't force load all of the file into memory
      reader = new FileReader(new File(fileName));
      StringWriter writer = new StringWriter();
      int chr = reader.read();
      while (chr != -1) {
        writer.write(chr);
        chr = reader.read();
      }
      String source = writer.toString();
      int start = compiler.originalBatchNode.getAbsolutePosition();
      int length = compiler.originalBatchNode.getLength();
      source =
        source.substring(0, start) +
        compiler.compiledBatchNode.toSource() +
        source.substring(start + length);
      System.out.println(source);
    } else {
      System.out.println("No batch statement found");
    }
  }

  public AstNode compiledBatchNode;
  public AstNode originalBatchNode;

  public boolean visit(AstNode node) {
    if (!(node instanceof BatchLoop)) {
      return true;
    }
    compiledBatchNode = new Scope();
    compiledBatchNode.addChild(new ExpressionStatement(JSUtil.genDeclare(
      "s$",
      new ObjectLiteral()
    )));

    BatchLoop batch = (BatchLoop)node;
    originalBatchNode = batch;
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
    AstNode preNode = null;
    String script = null;
    AstNode postNode = null;
    for (Stage stage : history) {
      switch (stage.place()) {
        case LOCAL:
          AstNode local = stage
            .action()
            .runExtra(new JSPartitionFactory())
            .Generate("r$", "s$");
          if (preNode == null && script == null) {
            preNode = local;
          } else {
            postNode = local;
          }
          break;
        case REMOTE:
          script = stage.action().runExtra(new FormatPartition());
          break;
      }
    }
    if (preNode != null) {
      compiledBatchNode.addChild(preNode);
    }
    if (script != null) {
      compiledBatchNode.addChild(new ExpressionStatement(JSUtil.genDeclare(
        "script$",
        JSUtil.genStringLiteral(script)
      )));
      final AstNode _postNode = postNode;
      compiledBatchNode.addChild(new ExpressionStatement(JSUtil.genCall(
        JSUtil.genName(service),
        "execute",
        new ArrayList<AstNode>() {{
          add(JSUtil.genName("script$"));
          add(JSUtil.genName("s$"));
          add(new FunctionNode() {{
            addParam(JSUtil.genName("r$"));
            setBody(
              _postNode != null
              ? JSUtil.genBlock(_postNode)
              : new Block()
            );
          }});
        }}
      )));
    } else {
      noimpl();
    }
    return false;
  }

  private <E> E noimpl() {
    throw new RuntimeException("Not yet implemented");
  }
}
