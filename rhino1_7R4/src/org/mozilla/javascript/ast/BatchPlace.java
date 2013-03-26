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

public enum BatchPlace {
  REMOTE,
  LOCAL;

  public static BatchPlace fromToken(int token) {
    if (token == Token.BATCH_REMOTE) {
      return REMOTE;
    } else if (token == Token.BATCH_LOCAL) {
      return LOCAL;
    }
    return null;
  }

  public String toKeyword() {
    switch (this) {
      case REMOTE: return "remote";
      case LOCAL : return "local";
      default    : return null;
    }
  }
}
