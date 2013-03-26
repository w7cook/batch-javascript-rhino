package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class BatchParam extends AstNode {

  private BatchPlace place;
  private AstNode param;

  public BatchParam(int pos, BatchPlace place, AstNode param) {
    super(pos);
    setPlace(place);
    setParameter(param);
  }

  public BatchPlace getPlace() {
    return place;
  }

  public void setPlace(BatchPlace place) {
    this.place = place;
  }

  public AstNode getParameter() {
    return param;
  }

  public void setParameter(AstNode param) {
    this.param = param;
    setLength(param.getPosition() + param.getLength() - getPosition());
    param.setParent(this);
  }

  @Override
  public void visit(NodeVisitor v) {
    if (v.visit(this)) {
      v.visit(param);
    }
  }

  @Override
  public String toSource(int depth) {
    return makeIndent(depth) + place.toKeyword() + " " + param.toSource(0);
  }
}
