package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class BatchParam extends AstNode {

  private String place;
  private AstNode param;

  public BatchParam(int pos, String place, AstNode param) {
    super(pos);
    setPlace(place);
    setParameter(param);
  }

  public String getPlace() {
    return place;
  }

  public void setPlace(String place) {
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
    return makeIndent(depth) + place.toLowerCase() + " " + param.toSource(0);
  }
}
