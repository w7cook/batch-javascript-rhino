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
    batchNodes.clear();
    batchNodes.addAll(compileBatchFunctions());
    batchNodes.addAll(compileBatchLoops());
    // Guarentee order of nodes
    Collections.sort(batchNodes, new PositionComparator());
    return batchNodes;
  }

  private List<TargetBatch<BatchFunction>> compileBatchFunctions() {
    batchFunctionsMap.clear();
    for (TargetBatch<BatchFunction> batchFunc: batchFunctionNodes) {
      FunctionNode func = batchFunc.original.getFunctionNode();
      if (func.getName().equals("")) {
        throw new Error("Batch functions must be given a name");
      }
        // TODO: put below in JSUtil.getParamPlaces()
        //if (!(param instanceof BatchParam)) {
        //  throw new Error(
        //    "All batch function parameters must be marked as either remote "
        //    + "or local"
        //  );
        //}
      batchFunctionsMap.put(
        func.getName(),
        func
      );
    }

    for (TargetBatch<BatchFunction> batchFunc: batchFunctionNodes) {
      FunctionNode func = batchFunc.original.getFunctionNode();

      CodeModel.factory.allowAllTransers = true;
      PExpr origExpr =
        new JSToPartition<PExpr>(CodeModel.factory, null, batchFunctionsMap)
          .exprFrom(func.getBody());
      Environment env = new Environment(CodeModel.factory);
      for (AstNode astParam : func.getParams()) {
        //BatchParam param = (BatchParam)astParam;
        //env = env.extend(
        //  JSUtil.mustIdentifierOf(param.getParameter()),
        //  null,
        //  param.getPlace()
        //);
        env = env.extend(
          JSUtil.mustIdentifierOf(astParam),
          null,
          Place.REMOTE
        );
      }
      History history = origExpr.partition(Place.MOBILE, env);
      AstNode preNode = null;
      AstNode script = null;
      AstNode postNode = null;
      final JSScriptConstructor _sc = new JSScriptConstructor("f$");
      for (Stage stage : history) {
        switch (stage.place()) {
          case LOCAL:
            Generator local = stage
              .action()
              .runExtra(new JSPartitionFactory());
            if (preNode == null && script == null) {
              preNode = local.Generate(null, "s$");
            } else {
              postNode = local
                .Bind(new JSGenFunction<AstNode>() {
                  public AstNode Generate(String in, String out, final AstNode _result) {
                    return new FunctionCall() {{
                      setTarget(JSUtil.genName("callback"));
                      // TODO: pass undefined if not returning an expression
                      addArgument(_result);
                    }};
                  }
                })
                .Generate("r$", null);
            }
            break;
          case REMOTE:
            script = stage.action().runExtra(_sc);
            break;
        }
      }
      final AstNode _preNode = preNode;
      final AstNode _script = script;
      final AstNode _postNode = postNode;
      final FunctionNode _func = func;
      AstNode remoteFunc = new FunctionNode() {{
        setFunctionName(JSUtil.genName(
          _func.getFunctionName().getIdentifier() + "$getRemote"
        ));
        AstNode partialScript = _script;
        addParam(JSUtil.genName("s$"));
        addParam(JSUtil.genName("f$"));
        for (AstNode astParam : _func.getParams()) {
          //BatchParam param = (BatchParam)astParam;
          //String paramName = JSUtil.mustIdentifierOf(param.getParameter());
          String paramName = JSUtil.mustIdentifierOf(astParam);
          addParam(JSUtil.genName(paramName));
          partialScript = _sc.Let(
            paramName,
            JSUtil.genName(paramName),
            partialScript
          );
        }
        final AstNode _fullScript = partialScript;
        setBody(JSUtil.concatBlocks(
          _preNode,
          new ReturnStatement() {{
            setReturnValue(_fullScript);
          }}
        ));
      }};
      AstNode postLocalFunc = new FunctionNode() {{
        setFunctionName(JSUtil.genName(
          _func.getFunctionName().getIdentifier() + "$postLocal"
        ));
        // TODO: local args
        addParam(JSUtil.genName("r$"));
        addParam(JSUtil.genName("callback"));
        setBody(JSUtil.genBlock(_postNode));
      }};

      batchFunc.compiled = JSUtil.concatBlocks(
        func,
        remoteFunc,
        postLocalFunc
      );
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
      loop.compiled.addChild(
        JSUtil.genDeclare(
          "f$",
          JSUtil.genCall(
            JSUtil.genName(service),
            "getFactory"
          )
        )
      );
      CodeModel.factory.allowAllTransers = true;
      PExpr origExpr =
        new JSToPartition<PExpr>(CodeModel.factory, root, batchFunctionsMap)
          .exprFrom(batch.getBody());
      Environment env = new Environment(CodeModel.factory)
        .extend(CodeModel.factory.RootName(), null, Place.REMOTE);
      History history = origExpr.partition(Place.MOBILE, env);
      AstNode preNode = null;
      AstNode script = null;
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
            script = stage.action().runExtra(new JSScriptConstructor("f$"));
            break;
        }
      }
      if (preNode != null) {
        loop.compiled.addChild(JSUtil.genStatement(preNode));
      }
      if (script != null) {
        loop.compiled.addChild(JSUtil.genDeclare(
          "script$",
          script
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
