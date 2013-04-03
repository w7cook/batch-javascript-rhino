var __BATCH_SERVICE__;

window.onload = (function(old_onload) {
  "use strict";
  return function() {

    /**
     * Incrementally builds object as subsequent portions of JSON is given.
     * NOTE: assumes numbers are not split across multiple append calls
     *
     * onchild is called when starting to parse an
     *   element of an array or value of an object's property
     *
     * onparse is called when finished parsing this value.
     *
     * append returns the leftover string that is not consumed.
     *
     */
    function JSONStream() {
      this.onparse = function(value) {};
      this.onchild = function(key, child) {};
      this.curr_child = undefined;
      this.data = undefined;
      this.children = [];
      this.initData = function(data) {};
    }
    JSONStream.prototype = {
      append: function(next_part) {
        "use strict";
        if (this.incomplete_part) {
          next_part = this.incomplete_part + next_part;
          this.incomplete_part = '';
        }
        var prev;
        do {
          if (this.finished) {
            return next_part;
          }
          prev = next_part;
          (
               (next_part = this.parseStartObject(next_part))
            && (next_part = this.parseKey(next_part))
            && (next_part = this.parseValue(next_part))
            && (next_part = this.parseStartArray(next_part))
            && (next_part = this.parseArrayElement(next_part))
            && !this.finished && (next_part = this.parseEndObject(next_part))
            && !this.finished && (next_part = this.parseEndArray(next_part))

            && !this.started

            && !this.finished && (next_part = this.parseTrue(next_part))
            && !this.finished && (next_part = this.parseFalse(next_part))
            && !this.finished && (next_part = this.parseNull(next_part))
            && !this.finished && (next_part = this.parseString(next_part))
            && !this.finished && (next_part = this.parseFloat(next_part))
          );
          if (this.finished) {
            this.onparse(this.key_path, this.data);
          }
        } while (next_part !== prev);
        if (!this.finished) {
          this.incomplete_part = next_part;
          return '';
        } else {
          return next_part;
        }
      },

      parseStartObject: function(next_part) {
        var match;
        if (!this.started
            && (match = next_part.match(/^\s*{(.*)$/))) {
          this.started = 'object';
          this.data = {};
          this.initData(this.data);
          return match[1];
        }
        return next_part;
      },

      parseKey: function(next_part) {
        var match;
        if (this.started === 'object'
            && !this.key
            && (match = next_part.match(/^\s*"((\\"|[^"])*[^\\])"\s*:(.*)$/))) {
          this.key = match[1];
          return match[3];
        }
        return next_part;
      },

      parseValue: function(next_part) {
        var match;
        if (this.key) {
          if (!this.curr_child) {
            this.curr_child = new JSONStream();
            this.curr_child.initData = (function(key, data) {
              this.data[key] = data;
            }).bind(this, this.key);
            this.children[this.key] = this.curr_child;
            this.onchild(this.key, this.curr_child);
          }
          if (!this.curr_child.finished) {
            next_part = this.curr_child.append(next_part);
          }
          if (this.curr_child.finished
              && (match = next_part.match(/^\s*(,|})(.*)$/))) {
            this.curr_child = undefined;
            this.key = undefined;
            if (match[1] === '}') {
              return this.parseEndObject(next_part);
            }
            return match[2];
          }
        }
        return next_part;
      },

      parseEndObject: function(next_part) {
        var match;
        if (this.started === 'object'
            && !this.key
            && (match = next_part.match(/^\s*}(.*)$/))) {
          this.started = false;
          this.finished = true;
          return match[1];
        }
        return next_part;
      },

      parseStartArray: function(next_part) {
        var match;
        if (!this.started
            && (match = next_part.match(/^\s*\[(.*)$/))) {
          this.started = 'array';
          this.index = 0;
          this.data = [];
          this.initData(this.data);
          return match[1];
        }
        return next_part;
      },

      parseArrayElement: function(next_part) {
        var match;
        if (this.started === 'array') {
          if (!this.curr_child && !next_part.match(/^\s*]$/)) {
            // started child
            this.curr_child = new JSONStream();
            this.curr_child.initData = (function(index, data) {
              this.data[index] = data;
            }).bind(this, this.index);
            this.children[this.index] = this.curr_child;
            this.onchild(this.index, this.curr_child);
          }
          if (this.curr_child) {
            if (!this.curr_child.finished) {
              next_part = this.curr_child.append(next_part);
            }
            if (this.curr_child.finished
                && (match = next_part.match(/^\s*(,|])(.*)$/))) {
              this.curr_child = undefined;
              this.index++;
              if (match[1] === ']') {
                return this.parseEndArray(next_part);
              }
              return match[2];
            }
          }
        }
        return next_part;
      },

      parseEndArray: function(next_part) {
        var match;
        if (this.started === 'array'
            && (match = next_part.match(/^\s*](.*)$/))) {
          this.started = false;
          this.finished = true;
          return match[1];
        }
        return next_part;
      },

      parseTrue: function(next_part) {
        var match = next_part.match(/^\s*true(.*)$/);
        if (match) {
          this.data = true;
          this.initData(this.data);
          this.finished = true;
          return match[1];
        }
        return next_part;
      },

      parseFalse: function(next_part) {
        var match = next_part.match(/^\s*false(.*)$/);
        if (match) {
          this.data = false;
          this.initData(this.data);
          this.finished = true;
          return match[1];
        }
        return next_part;
      },

      parseNull: function(next_part) {
        var match = next_part.match(/^\s*null(.*)$/);
        if (match) {
          this.data = null;
          this.initData(this.data);
          this.finished = true;
          return match[1];
        }
        return next_part;
      },

      parseString: function(next_part) {
        var match = next_part.match(/^\s*("(\\"|[^"])*[^\\]")(.*)$/);
        if (match) {
          this.data = JSON.parse(match[1]);
          this.initData(this.data);
          this.finished = true;
          return match[3];
        }
        return next_part;
      },

      parseFloat: function(next_part) {
        var match = next_part.match(/^\s*([-0-9][0-9.eE+-]*)(.*)$/);
        if (match) {
          var f = parseFloat(match[1]);
          this.data = f;
          this.initData(this.data);
          this.finished = true;
          return match[2];
        }
        return next_part;
      }

    };

    function stripBraces(s) {
      if (s.charAt(0) === "{") {
        return s.substring(1, s.length - 1);
      } else {
        return s;
      }
    }

    var unaryOps = {
      "+": true,
      "-": true,
      "/": true,
      "not": true,
    };

    var FormatFactory = {
      Var: function(name) { return name; },
      Data: function(data) { return JSON.stringify(data); },
      Fun: function(varName, body) { return "{function("+varName+") "+body+"}"; },
      Prop: function(base, field) { return base+"."+field; },
      Assign: function(target, source) { return target+"="+source; },
      Let: function(varName, expression, body) {
        return "{var " + varName + "=" + expression + "; " + stripBraces(body) + "}";
      },
      If: function(condition, thenExp, elseExp) {
        if (elseExp === "skip")
          return "{if (" + condition + ") " + thenExp + "}";
        else
          return "{if (" + condition + ") " + thenExp + " else " + elseExp + "}";
      },
      Loop: function(varName, collection, body) {
        return "{for (" + varName + " in " + collection + ") " + body + "}";
      },
      Call: function(target, method, args) { return target+"."+method+"("+args.join(", ")+")"; },
      In: function(location) { return 'INPUT("'+location+'")'; },
      Out: function(location, expression) { return 'OUTPUT("'+location+'", '+expression+')'; },
      Prim: function(op, args) {
        if (args.length === 0) {
          return "skip";
        } if (args.length === 1 && unaryOps[op]) {
          return "(" + op + args[0] + ")";
        } else {
          if (op === ";") {
            args = args.map(stripBraces);
          }
          var s = args.join(" "+op+" ");
          if (op === ";") {
            return "{"+s+"}";
          } else {
            return "("+s+")";
          }
        }
      },
    };

    // onload actions
    if (old_onload) old_onload.apply(window, arguments);

    function AsyncObject(jsonStream) {
      this.jsonStream = jsonStream;
      var onchild = (function (key, child) {
        child.onparse = this.tryCallback.bind(this);
        child.onchild = onchild;
      }).bind(this);
      this.jsonStream.onchild = onchild;
      this.returnObject = {
        asyncForEach: (function(loop_var, step_cb, post_cb) {
          if (this.callback) {
            throw new Error('Multiple async-waits not implemented');
          }
          this.waiting_for = loop_var;
          this.startLoop = (function(loop) {
            this.startLoop = undefined;
            var i = 0;
            var next = this.stepLoop = (function() {
              this.stepLoop = undefined;
              if (i < loop.children.length) {
                step_cb(
                  new AsyncObject(loop.children[i++]).returnObject,
                  next
                );
              } else if (loop.finished) {
                post_cb(false, undefined);
              } else {
                this.stepLoop = next;
              }
            }).bind(this);
            return this.stepLoop();
          }).bind(this);
          this.tryCallback();
        }).bind(this),

        get: (function(var_name, cb) {
          if (this.callback) {
            throw new Error('Multiple async-waits not implemented');
          }
          this.waiting_for = var_name;
          this.callback = (function(value) {
            this.callback = undefined;
            cb(value);
          }).bind(this);
          this.tryCallback();
        }).bind(this)
      };
    }
    AsyncObject.prototype = {
      tryCallback: function() {
        if (this.callback
            && typeof this.jsonStream.data === 'object'
            && (this.waiting_for in this.jsonStream.data)) {
          this.callback(this.jsonStream.data[this.waiting_for]);
        }
        if (this.waiting_for in this.jsonStream.children) {
          if (this.startLoop) {
            this.startLoop(this.jsonStream.children[this.waiting_for]);
          }
          if (this.stepLoop) {
            this.stepLoop();
          }
        }
      },
      appendMsg: function(next_part) {
        // check version
        if (this.version === undefined) {
          if (this.incomplete_version) {
            next_part = this.incomplete_version + next_part;
            this.incomplete_version = '';
          }
          var version_and_rest = next_part.split('\n', 2);
          if (version_and_rest.length > 0) {
            this.version = version_and_rest[0];
            if (this.version !== 'Batch 1.0 JSON 1.0') {
              throw new Error(
                'Incompatible batch message format: ' + this.version
              );
            }
            next_part = version_and_rest[1];
          } else {
            this.incomplete_version = next_part;
            return;
          }
        }
        next_part = (next_part || '').trim();
        if (this.version && next_part) {
          this.jsonStream.append(next_part);
        }
      }
    };

    var __next_id = 0;
    function getNextID() {
      return __next_id++;
    }

    var websocket = new WebSocket('ws://localhost:9999');
    var callbacks = {};
    var asyncObjects = {};
    websocket.onmessage = function(event) {
      var parts = event.data.split('\n', 2);
      var id = parts[0];
      var result = parts[1];
      if (callbacks[id]) {
        var cb = callbacks[id];
        callbacks[id] = undefined;
        cb(asyncObjects[id].returnObject);
      }
      asyncObjects[id].appendMsg(result);
    }
    websocket.onerror = function(event) {
      throw new Error(event.data);
    }

    __BATCH_SERVICE__ = {
      getFactory: function() {
        return FormatFactory;
      },
      execute: function(script, data, callback) {
        var id = getNextID();
        callbacks[id] = callback;
        asyncObjects[id] = new AsyncObject(new JSONStream());
        function try_send() {
          if (websocket.readyState != WebSocket.OPEN) {
            setTimeout(try_send, 10);
          } else {
            websocket.send(
              id + '\n' +
              script + '\n\n' +
              'BATCH 1.0 JSON 1.0\n' +
              JSON.stringify(data)
            );
          }
        }
        try_send();
      }
    }

  };
})(window.onload);
