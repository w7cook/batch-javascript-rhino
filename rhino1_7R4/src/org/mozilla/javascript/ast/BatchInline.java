package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class BatchInline extends AstNode {

  {
    type = Token.BATCH_INLINE;
  }

  private Name funcNameToInline;

  public BatchInline(int pos, Name func) {
    super(pos);
    funcNameToInline = func;
    setLength(func.getPosition() + func.getLength() - pos);
    func.setParent(this);
  }

  public Name getFunctionName() {
    return funcNameToInline;
  }

  public void setFunctionName(Name func) {
    funcNameToInline = func;
  }

  @Override
  public void visit(NodeVisitor v) {
    if (v.visit(this)) {
      v.visit(funcNameToInline);
    }
  }

  @Override
  public String toSource(int depth) {
    return makeIndent(depth) + "batch " + funcNameToInline.toSource(0);
  }
}
