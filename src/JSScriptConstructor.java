import batch.Op;
import batch.partition.PartitionFactory;
import batch.util.BatchFactoryHelper;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NumberLiteral;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class JSScriptConstructor implements PartitionFactory<AstNode> {

  private String factoryName;

  private AstNode factory() {
    return JSUtil.genName(factoryName);
  }

  public JSScriptConstructor(String factory) {
    factoryName = factory;
  }

  @Override
  public AstNode Var(String name) {
    return JSUtil.genCall(
      factory(),
      "Var",
      JSUtil.genStringLiteral(name)
    );
  }

  @Override
  public AstNode Data(Object value) {
    AstNode literal;
    if (value instanceof Number) {
      literal = new NumberLiteral(((Number)value).doubleValue());
    } else if (value instanceof String) {
      literal = JSUtil.genStringLiteral((String)value);
    } else {
      return JSUtil.noimpl();
    }
    return JSUtil.genCall(
      factory(),
      "Data",
      literal
    );
  }

  @Override
  public AstNode Fun(String var, AstNode body) {
    return JSUtil.genCall(
      factory(),
      "Fun",
      JSUtil.genStringLiteral(var),
      body
    );
  }

  @Override
  public AstNode Prim(Op op, List<AstNode> args) {
    return JSUtil.genCall(
      factory(),
      "Prim",
      // TODO: maybe create JS representation
      JSUtil.genStringLiteral(op.getOpSymbol()),
      JSUtil.genArray(args)
    );
  }

  @Override
  public AstNode Prop(AstNode base, String field) {
    return JSUtil.genCall(
      factory(),
      "Prop",
      base,
      JSUtil.genStringLiteral(field)
    );
  }

  @Override
  public AstNode Assign(AstNode target, AstNode source) {
    return JSUtil.genCall(
      factory(),
      "Assign",
      target,
      source
    );
  }

  @Override
  public AstNode Let(String var, AstNode expression, AstNode body) {
    return JSUtil.genCall(
      factory(),
      "Let",
      JSUtil.genStringLiteral(var),
      expression,
      body
    );
  }

  @Override
  public AstNode If(AstNode condition, AstNode thenExp, AstNode elseExp) {
    return JSUtil.genCall(
      factory(),
      "If",
      condition,
      thenExp,
      elseExp
    );
  }

  @Override
  public AstNode Loop(String var, AstNode collection, AstNode body) {
    return JSUtil.genCall(
      factory(),
      "Loop",
      JSUtil.genStringLiteral(var),
      collection,
      body
    );
  }

  @Override
  public AstNode Call(AstNode target, String method, List<AstNode> args) {
    return JSUtil.genCall(
      factory(),
      "Call",
      target,
      JSUtil.genStringLiteral(method),
      JSUtil.genArray(args)
    );
  }

  @Override
  public AstNode In(String location) {
    return JSUtil.genCall(
      factory(),
      "In",
      JSUtil.genStringLiteral(location)
    );
  }

  @Override
  public AstNode Out(String location, AstNode expression) {
    return JSUtil.genCall(
      factory(),
      "Out",
      JSUtil.genStringLiteral(location),
      expression
    );
  }

  @Override
  public AstNode DynamicCall(
      AstNode target,
      final String _method,
      final List<AstNode> _args) {
    final AstNode _factory = factory();
    return new FunctionCall() {{
      setTarget(JSUtil.genName(_method + "$getRemote"));
      List<AstNode> allArgs = new ArrayList<AstNode>() {{
        add(JSUtil.genName("s$"));
        add(_factory);
        addAll(_args);
      }};
      setArguments(allArgs);
    }};
  }

  @Override
  public AstNode Other(Object external, List<AstNode> subs) {
    return JSUtil.noimpl();
  }

  @Override
  public AstNode Mobile(String type, AstNode exp) {
    return JSUtil.noimpl();
  }

  @Override
  public AstNode setExtra(AstNode exp, Object extra) {
    return exp;
  }

  // Helpers
	@SuppressWarnings("unchecked")
  @Override
	public AstNode Prim(Op op, Object... args) {
		List<AstNode> argList = new ArrayList<AstNode>();
		for (Object arg : args)
			argList.add((AstNode)arg);
		return Prim(op, argList);
	}

	@SuppressWarnings("unchecked")
  @Override
	public AstNode Call(AstNode target, String method, Object... args) {
		List<AstNode> argList = new ArrayList<AstNode>();
		for (Object arg : args)
			argList.add((AstNode)arg);
		return Call(target, method, argList);
	}

	@Override
	public String RootName() {
		return BatchFactoryHelper.ROOT_VAR_NAME;
	}

	@Override
	public AstNode Root() {
		return Var(RootName());
	}

  @Override
  public AstNode Skip() {
    return Prim(Op.SEQ, Collections.emptyList());
  }

	@Override
	public AstNode Other(Object external, AstNode... subs) {
		List<AstNode> subList = new ArrayList<AstNode>();
		for (AstNode e : subs)
			subList.add(e);
		return Other(external, subList);
	}
}

