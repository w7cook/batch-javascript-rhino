package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class BatchFunction extends AstNode {

  {
    type = Token.BATCH_FUNCTION;
  }

  private FunctionNode functionNode;

  public BatchFunction(int pos, FunctionNode func) {
    super(pos);
    functionNode = func;
    setLength(func.getPosition() + func.getLength() - pos);
    func.setParent(this);
  }

  public FunctionNode getFunctionNode() {
    return functionNode;
  }

  public void setFunctionNode(FunctionNode func) {
    functionNode = func;
  }

  @Override
  public void visit(NodeVisitor v) {
    if (v.visit(this)) {
      v.visit(functionNode);
    }
  }

  @Override
  public String toSource(int depth) {
    return makeIndent(depth) + "batch " + functionNode.toSource(0);
  }
}
