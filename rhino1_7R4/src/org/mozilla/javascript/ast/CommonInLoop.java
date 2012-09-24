/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This file is a modified version of ForInLoop.java
 */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * Common code for loop with "in" keyword.
 */
public abstract class CommonInLoop extends Loop {

    protected AstNode iterator;
    protected AstNode iteratedObject;
    protected int inPosition = -1;

    public CommonInLoop() {
    }

    public CommonInLoop(int pos) {
        super(pos);
    }

    public CommonInLoop(int pos, int len) {
        super(pos, len);
    }

    /**
     * Returns loop iterator expression
     */
    public AstNode getIterator() {
        return iterator;
    }

    /**
     * Sets loop iterator expression:  the part before the "in" keyword.
     * Also sets its parent to this node.
     * @throws IllegalArgumentException if {@code iterator} is {@code null}
     */
    public void setIterator(AstNode iterator) {
        assertNotNull(iterator);
        this.iterator = iterator;
        iterator.setParent(this);
    }

    /**
     * Returns object being iterated over
     */
    public AstNode getIteratedObject() {
        return iteratedObject;
    }

    /**
     * Sets object being iterated over, and sets its parent to this node.
     * @throws IllegalArgumentException if {@code object} is {@code null}
     */
    public void setIteratedObject(AstNode object) {
        assertNotNull(object);
        this.iteratedObject = object;
        object.setParent(this);
    }

    /**
     * Returns position of "in" keyword
     */
    public int getInPosition() {
        return inPosition;
    }

    /**
     * Sets position of "in" keyword
     * @param inPosition position of "in" keyword,
     * or -1 if not present (e.g. in presence of a syntax error)
     */
    public void setInPosition(int inPosition) {
        this.inPosition = inPosition;
    }

    /**
     * Visits this node, the iterator, the iterated object, and the body.
     */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            iterator.visit(v);
            iteratedObject.visit(v);
            body.visit(v);
        }
    }
}
