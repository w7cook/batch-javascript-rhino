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

import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO Future: allow batch functions to be referred to across files
public class BatchCompiler implements NodeVisitor {
  public static void main(String[] args)
      throws FileNotFoundException, IOException {
    String fileName = args[0];
    FileReader reader = new FileReader(new File(fileName));
    Parser parser = new Parser();
    // TODO Extra: only parse batch code
    AstRoot ast = parser.parse(reader, fileName, /*linenumber*/ 0);
    BatchCompiler compiler = new BatchCompiler();
    ast.visit(compiler);

    String source = new String(
      Files.readAllBytes(new File(fileName).toPath())
    );
    StringWriter result = new StringWriter();
    int offset = 0;
    for (TargetBatch<? extends AstNode> batch : compiler.compileBatches()) {
      int start = batch.original.getAbsolutePosition();
      int length = batch.original.getLength();
      result.write(source, offset, start - offset);
      result.write(batch.compiled.toSource());
      offset = start + length;
    }
    result.write(source, offset, source.length() - offset);
    System.out.println(result.toString());
  }

  // Ordered by absolute position in source
  private List<TargetBatch<? extends AstNode>> batchNodes = new ArrayList<>();
  private List<TargetBatch<BatchLoop>> batchLoops = new ArrayList<>();
  private List<TargetBatch<BatchFunction>> batchFunctionNodes =
    new ArrayList<>();
  private Map<String, FunctionNode> batchFunctionsMap = new HashMap<>();

  public boolean visit(AstNode node) {
    if (node instanceof BatchLoop) {
      batchLoops.add(new TargetBatch<BatchLoop>((BatchLoop)node, null));
      return false;
    } else if (node instanceof BatchFunction) {
      batchFunctionNodes.add(
        new TargetBatch<BatchFunction>((BatchFunction)node, null)
      );
      return false;
    } else {
      return true;
    }
  }

  public Iterable<TargetBatch<? extends AstNode>> compileBatches() {
    if (batchNodes.isEmpty()) {
      batchNodes.addAll(compileBatchFunctions());
      batchNodes.addAll(compileBatchLoops());
    }
    // Guarentee order of nodes
    Collections.sort(batchNodes, new PositionComparator());
    return batchNodes;
  }

  private List<TargetBatch<BatchFunction>> compileBatchFunctions() {
    if (batchFunctionsMap.isEmpty()) {
      for (TargetBatch<BatchFunction> batchFunc: batchFunctionNodes) {
        FunctionNode func = batchFunc.original.getFunctionNode();
        batchFunc.compiled = func;
        if (func.getName().equals("")) {
          throw new Error("Batch functions must be given a name");
        }
        batchFunctionsMap.put(
          func.getName(),
          func
        );
      }
    }
    return batchFunctionNodes;
  }

  private List<TargetBatch<BatchLoop>> compileBatchLoops() {
    for (TargetBatch<BatchLoop> loop : batchLoops) {
      loop.compiled = new Scope();
      loop.compiled.addChild(JSUtil.genDeclare(
        "s$",
        new ObjectLiteral()
      ));

      BatchLoop batch = loop.original;
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
          JSUtil.noimpl();
      }
      String service = null;
      switch (batch.getIteratedObject().getType()) {
        case Token.NAME:
          service = ((Name)batch.getIteratedObject()).getIdentifier();
          break;
        default:
          JSUtil.noimpl();
      }
      CodeModel.factory.allowAllTransers = true;
      PExpr origExpr =
        new JSToPartition<PExpr>(CodeModel.factory, root, batchFunctionsMap)
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
        loop.compiled.addChild(JSUtil.genStatement(preNode));
      }
      if (script != null) {
        loop.compiled.addChild(JSUtil.genDeclare(
          "script$",
          JSUtil.genStringLiteral(script)
        ));
        final AstNode _postNode = postNode;
        loop.compiled.addChild(new ExpressionStatement(JSUtil.genCall(
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
        JSUtil.noimpl();
      }
    }
    return batchLoops;
  }

  class TargetBatch<O> {
    public O original;
    public AstNode compiled;
    public TargetBatch(O o, AstNode c) {
      original = o;
      compiled = c;
    }
  }

  class PositionComparator
      implements Comparator<TargetBatch<? extends AstNode>> {
    public int compare(
        TargetBatch<? extends AstNode> x,
        TargetBatch<? extends AstNode> y
    ) {
      return x.original.getAbsolutePosition()
           - y.original.getAbsolutePosition();
    }
  }
}
