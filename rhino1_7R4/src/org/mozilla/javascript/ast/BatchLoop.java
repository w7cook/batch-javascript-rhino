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
 * Batch statement.  Node type is {@link Token#BATCH}.<p>
 *
 * <pre><b>batch</b> ( LeftHandSideExpression <b>in</b> Expression ) Statement</pre>
 * <pre><b>batch</b> ( <b>var</b> VariableDeclarationNoIn <b>in</b> Expression ) Statement</pre>
 */
public class BatchLoop extends CommonInLoop {

    {
        type = Token.BATCH;
    }

    public BatchLoop() {
    }

    public BatchLoop(int pos) {
        super(pos);
    }

    public BatchLoop(int pos, int len) {
        super(pos, len);
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("batch (");
        sb.append(iterator.toSource(0));
        sb.append(" in ");
        sb.append(iteratedObject.toSource(0));
        sb.append(") ");
        if (body instanceof Block) {
            sb.append(body.toSource(depth).trim()).append("\n");
        } else {
            sb.append("\n").append(body.toSource(depth+1));
        }
        return sb.toString();
    }
}
