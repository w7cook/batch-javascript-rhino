package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class BatchExpression extends AstNode {

  {
    type = Token.BATCH;
  }

  private AstNode expression;

  public BatchExpression(int pos, AstNode expression) {
    super(pos);
    setExpression(expression);
  }

  public AstNode getExpression() {
    return expression;
  }

  public void setExpression(AstNode expression) {
    this.expression = expression;
    setLength(expression.getPosition() + expression.getLength() - getPosition());
    expression.setParent(this);
  }

  @Override
  public void visit(NodeVisitor v) {
    if (v.visit(this)) {
      v.visit(expression);
    }
  }

  @Override
  public String toSource(int depth) {
    return makeIndent(depth) + "batch " + expression.toSource(0);
  }
}
