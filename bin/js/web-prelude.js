function JSONStream(key_path) {
  this.onparse = function(key_path, value) {};
  this.key_path = key_path || [];
  this.data = undefined;
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
      var child_key_path = this.key_path.concat([this.key]);
      if (!this.curr_child) {
        this.curr_child = new JSONStream(child_key_path);
        this.curr_child.onparse = this.onparse;
      }
      next_part = this.curr_child.append(next_part);
      if (this.curr_child.finished
          && (match = next_part.match(/^\s*(,|})(.*)$/))) {
        this.data[this.key] = this.curr_child.data;
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
      return match[1];
    }
    return next_part;
  },

  parseArrayElement: function(next_part) {
    var match;
    if (this.started === 'array') {
      var child_key_path = this.key_path.concat([this.index]);
      if (!this.curr_child) {
        this.curr_child = new JSONStream(child_key_path);
        this.curr_child.onparse = this.onparse;
      }
      next_part = this.curr_child.append(next_part);
      if (this.curr_child.finished
          && (match = next_part.match(/^\s*(,|])(.*)$/))) {
        this.data[this.index] = this.curr_child.data;
        this.curr_child = undefined;
        this.index++;
        if (match[1] === ']') {
          return this.parseEndArray(next_part);
        }
        return match[2];
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
      this.finished = true;
      return match[1];
    }
    return next_part;
  },

  parseFalse: function(next_part) {
    var match = next_part.match(/^\s*false(.*)$/);
    if (match) {
      this.data = false;
      this.finished = true;
      return match[1];
    }
    return next_part;
  },

  parseNull: function(next_part) {
    var match = next_part.match(/^\s*null(.*)$/);
    if (match) {
      this.data = null;
      this.finished = true;
      return match[1];
    }
    return next_part;
  },

  parseString: function(next_part) {
    var match = next_part.match(/^\s*("(\\"|[^"])*[^\\]")(.*)$/);
    if (match) {
      this.data = JSON.parse(match[1]);
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
      this.finished = true;
      return match[2];
    }
    return next_part;
  }

};

var __BATCH_SERVICE__;
window.onload = (function(old_onload) {
  "use strict";
  return function() {
    if (old_onload) old_onload.apply(window, arguments);

    var __next_id = 0;
    function getNextID() {
      return __next_id++;
    }

    function asyncObject(data) {
      var callback, waiting_for;
      function tryCallback() {
        if (callback && (waiting_for in data)) {
          var cb = callback;
          callback = undefined;
          cb(data[waiting_for]);
        }
      }
      return {
        returnObject: {
          asyncForEach: function(loop_var, step_cb, post_cb) {
            if (callback) {
              throw new Error('Multiple async-waits not implemented');
            }
            waiting_for = loop_var;
            callback = function(val) {
              val = val.concat([]).reverse();
              function next() {
                if (val.length > 0) {
                  step_cb(asyncObject(val.pop()).returnObject, next);
                } else {
                  post_cb();
                }
              }
              return next();
            }
            tryCallback();
          },
          get: function(var_name, cb) {
            if (callback) {
              throw new Error('Multiple async-waits not implemented');
            }
            waiting_for = var_name;
            callback = cb;
            tryCallback();
          }
        },
        set: function(var_name, value) {
          data[var_name] = value;
          tryCallback();
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
            if (this.jsonStream === undefined) {
              this.jsonStream = new JSONStream();
              this.jsonStream.onparse = (function(key_path, data) {
                if (key_path.length === 1) {
                  this.set(key_path[0], data);
                }
                if (key_path.length === 0) {
                  // TODO clean up
                }
              }).bind(this);
            }
            this.jsonStream.append(next_part);
          }
        }
      };
    }

    var websocket = new WebSocket('ws://localhost:9999');
    var callbacks = {};
    var asyncObjects = {};
    websocket.onmessage = function(event) {
      var parts = event.data.split('\n', 2);
      var id = parts[0];
      var result = parts[1];
      asyncObjects[id].appendMsg(result);
      if (callbacks[id]) {
        var cb = callbacks[id];
        callbacks[id] = undefined;
        cb(asyncObjects[id].returnObject);
      }
    }
    websocket.onerror = function(event) {
      throw new Error(event.data);
    }

    __BATCH_SERVICE__ = {
      execute: function(script, data, callback) {
        var id = getNextID();
        callbacks[id] = callback;
        asyncObjects[id] = asyncObject({});
        function try_send() {
          if (websocket.readyState != WebSocket.OPEN) {
            setTimeout(try_send, 10);
          } else {
            websocket.send(id + '\n' + script);
          }
        }
        try_send();
      }
    }

  };
})(window.onload);
