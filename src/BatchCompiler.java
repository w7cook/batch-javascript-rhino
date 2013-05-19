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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// TODO Future: allow batch functions to be referred to across files
public class BatchCompiler implements NodeVisitor {
  public static void main(String[] args)
      throws FileNotFoundException, IOException {
    String fileName = args[0];
    FileReader reader = new FileReader(new File(fileName));
    Parser parser = new Parser();
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
  private List<TargetBatch<BatchExpression>> batchExprs = new ArrayList<>();
  private List<TargetBatch<BatchFunction>> batchFunctionNodes =
    new ArrayList<>();
  private Map<String, DynamicCallInfo> batchFunctionsInfo = new HashMap<>();

  public boolean visit(AstNode node) {
    if (node instanceof BatchExpression) {
      batchExprs.add(
        new TargetBatch<BatchExpression>((BatchExpression)node, null)
      );
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
    batchNodes.addAll(compileBatchExpressions());
    // Guarentee order of nodes
    Collections.sort(batchNodes, new PositionComparator());
    return batchNodes;
  }

  private List<TargetBatch<BatchFunction>> compileBatchFunctions() {
    batchFunctionsInfo.clear();
    for (TargetBatch<BatchFunction> batchFunc: batchFunctionNodes) {
      FunctionNode func = batchFunc.original.getFunctionNode();
      if (func.getName().equals("")) {
        throw new Error("Batch functions must be given a name");
      }

      List<Place> argPlaces = new ArrayList<Place>();
      for (AstNode param : func.getParams()) {
        if (!(param instanceof BatchParam)) {
          throw new Error(
            "All batch function parameters must be marked as either remote "
            + "or local"
          );
        }
        argPlaces.add(toPlace(((BatchParam)param).getPlace()));
      }
      batchFunctionsInfo.put(
        func.getName(),
        new DynamicCallInfo(
          toPlace(batchFunc.original.getReturnPlace()),
          argPlaces
        )
      );
    }

    for (TargetBatch<BatchFunction> batchFunc: batchFunctionNodes) {
      final FunctionNode _func = batchFunc.original.getFunctionNode();

      CodeModel.factory.allowAllTransers = true;
      final boolean _isRemoteFunction =
        batchFunctionsInfo.get(_func.getName()).returns == Place.REMOTE;
      final PExpr _origExpr =
        new JSToPartition<PExpr>(
            CodeModel.factory,
            null,
            _isRemoteFunction,
            batchFunctionsInfo
          ).exprFrom(_func.getBody());
      AstNode rawFunction = new FunctionNode() {{
        setFunctionName(_func.getFunctionName());
        for (AstNode param : _func.getParams()) {
          if (param instanceof BatchParam) {
            addParam(((BatchParam)param).getParameter());
          } else {
            addParam(param);
          }
        }
        setBody(JSUtil.genBlock(
          _origExpr
            .runExtra(new RawJSFactory())
            .Generate(null, null, new Function<AstNode, AstNode>() {
              public AstNode call(final AstNode _result) {
                return new ReturnStatement() {{
                  setReturnValue(_result);
                }};
              }
            })
        ));
      }};
      Environment env = new Environment(CodeModel.factory);
      for (AstNode astParam : _func.getParams()) {
        BatchParam param = (BatchParam)astParam;
        env = env.extend(
          JSUtil.mustIdentifierOf(param.getParameter()),
          null,
          toPlace(param.getPlace())
        );
      }
      History history = _origExpr.partition(Place.MOBILE, env);
      AstNode preNode = null;
      AstNode script = null;
      AstNode postNode = null;
      final JSScriptConstructor _sc = new JSScriptConstructor("f$");
      for (Stage stage : history) {
        switch (stage.place()) {
          case LOCAL:
            Generator local = stage
              .action()
              .runExtra(new LocalPartitionToJS(batchFunctionsInfo));
            if (preNode == null && script == null) {
              preNode = local.Generate(null, "s$", null);
            } else {
              postNode = local
                .Bind(Function.<AstNode,Generator>Const(
                  new ReturnGenerator(JSUtil.genUndefined())
                ))
                .Generate("r$", null, new Function<AstNode, AstNode>() {
                  public AstNode call(AstNode result) {
                    return JSUtil.genCall(
                      JSUtil.genName("callback$"),
                      result
                    );
                  }
                });
            }
            break;
          case REMOTE:
            script = stage.action().runExtra(_sc);
            break;
        }
      }
      if (postNode != null && _isRemoteFunction) {
        throw new Error(
          "Compiler error: remote batch functions should not have post "
          + "local code"
        );
      }
      final AstNode _preNode = preNode;
      final AstNode _script = script;
      final AstNode _postNode = postNode;
      AstNode remoteFunc = new FunctionNode() {{
        setFunctionName(JSUtil.genName(
          _func.getFunctionName().getIdentifier() + "$getRemote"
        ));
        AstNode partialScript = _script;
        addParam(JSUtil.genName("s$"));
        addParam(JSUtil.genName("f$"));
        Iterator<AstNode> argIt = _func.getParams().iterator();
        Iterator<Place> placeIt =
          batchFunctionsInfo.get(_func.getName()).arguments.iterator();
        while (argIt.hasNext()) {
          BatchParam param = (BatchParam)argIt.next();
          Place place = placeIt.next();

          String paramName = JSUtil.mustIdentifierOf(param.getParameter());
          addParam(JSUtil.genName(paramName));
          if (place == Place.REMOTE) {
            partialScript = _sc.Let(
              paramName,
              JSUtil.genName(paramName),
              partialScript
            );
          }
        }
        final AstNode _fullScript = partialScript;
        setBody(JSUtil.concatBlocks(
          _preNode,
          new ReturnStatement() {{
            setReturnValue(_fullScript);
          }}
        ));
      }};
      AstNode postLocalFunc = null;
      if (!_isRemoteFunction) {
        postLocalFunc = new FunctionNode() {{
          setFunctionName(JSUtil.genName(
            _func.getFunctionName().getIdentifier() + "$postLocal"
          ));
          addParam(JSUtil.genName("r$"));
          // add local params
          Iterator<AstNode> argIt = _func.getParams().iterator();
          Iterator<Place> placeIt =
            batchFunctionsInfo.get(_func.getName()).arguments.iterator();
          while (argIt.hasNext()) {
            BatchParam param = (BatchParam)argIt.next();
            if (placeIt.next() != Place.REMOTE) {
              String paramName = JSUtil.mustIdentifierOf(param.getParameter());
              addParam(JSUtil.genName(paramName));
            }
          }
          addParam(JSUtil.genName("callback$")); // TODO: avoid conflicts
          setBody(
            _postNode != null
              ? JSUtil.genBlock(_postNode)
              : JSUtil.concatBlocks()
          );
        }};
      }

      batchFunc.compiled = JSUtil.concatBlocks(
        rawFunction,
        remoteFunc,
        postLocalFunc
      );
    }
    return batchFunctionNodes;
  }

  private List<TargetBatch<BatchExpression>> compileBatchExpressions() {
    for (TargetBatch<BatchExpression> expr : batchExprs) {
      expr.compiled = new Scope();
      expr.compiled.addChild(JSUtil.genDeclare(
        "s$",
        new ObjectLiteral()
      ));

      BatchExpression batch = expr.original;
      if (!(batch.getExpression() instanceof FunctionCall)) {
        return invalidBatchBlock();
      }
      FunctionCall call = (FunctionCall)batch.getExpression();
      if (!(call.getTarget() instanceof PropertyGet)
          || call.getArguments().size() != 1
          || !(call.getArguments().get(0) instanceof FunctionNode)) {
        return invalidBatchBlock();
      }
      PropertyGet prop = (PropertyGet)call.getTarget();
      String service = JSUtil.identifierOf(prop.getTarget());
      FunctionNode callback = (FunctionNode)call.getArguments().get(0);
      if (service == null
          || !prop.getProperty().getIdentifier().equals("execute")
          || callback.getParams().size() != 1) {
        return invalidBatchBlock();
      }
      String root = JSUtil.identifierOf(callback.getParams().get(0));
      if (root == null) {
        return invalidBatchBlock();
      }
      expr.compiled.addChild(
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
        new JSToPartition<PExpr>(
            CodeModel.factory,
            root,
            false,
            batchFunctionsInfo
          ).exprFrom(callback.getBody());
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
              .runExtra(new LocalPartitionToJS(batchFunctionsInfo))
              .Generate("r$", "s$", null);
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
        expr.compiled.addChild(JSUtil.genStatement(preNode));
      }
      if (script != null) {
        expr.compiled.addChild(JSUtil.genDeclare(
          "script$",
          script
        ));
        final AstNode _postNode = postNode;
        expr.compiled.addChild(new ExpressionStatement(JSUtil.genCall(
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
    return batchExprs;
  }

  private static <E> E invalidBatchBlock() {
    throw new Error(
      "Batch blocks must be of the form: "
      + "batch <SERVICE>.execute(function(<ROOT>) {...}"
    );
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

  private static Place toPlace(BatchPlace place) {
    switch (place) {
      case REMOTE: return Place.REMOTE;
      case LOCAL: return Place.LOCAL;
      default: throw new Error("Invalid place: "+place);
    }
  }
}
