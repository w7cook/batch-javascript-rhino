package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class BatchFunction extends AstNode {

  private BatchPlace returnPlace;
  private FunctionNode functionNode;

  public BatchFunction(int pos, int place, FunctionNode func) {
    super(pos);
    type = place;
    returnPlace = BatchPlace.fromToken(place);
    setFunctionNode(func);
  }

  public BatchPlace getReturnPlace() {
    return returnPlace;
  }

  public void setReturnPlace(BatchPlace place) {
    returnPlace = place;
  }

  public FunctionNode getFunctionNode() {
    return functionNode;
  }

  public void setFunctionNode(FunctionNode func) {
    functionNode = func;
    setLength(func.getPosition() + func.getLength() - getPosition());
    func.setParent(this);
  }

  @Override
  public void visit(NodeVisitor v) {
    if (v.visit(this)) {
      v.visit(functionNode);
    }
  }

  @Override
  public String toSource(int depth) {
    return makeIndent(depth)
           + returnPlace.toKeyword() + " " + functionNode.toSource(0);
  }
}
