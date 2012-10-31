var __BATCH_SERVICE__;
window.onload = (function(old_onload) {
  return function() {
    if (old_onload) old_onload.apply(window, arguments);

    function parseMsg(s) {
      var firstLineIndex = s.indexOf('\n');
      if (s.slice(0, firstLineIndex) !== 'Batch 1.0 JSON 1.0') {
        throw new Error(
          'Invalid batch message format: ' + s.slice(0, firstLineIndex)
        );
      } else {
        return JSON.parse(s.slice(firstLineIndex + 1));
      }
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
        }
      };
    }

    var websocket = new WebSocket('ws://localhost:9999');
    var callbacks = [];
    websocket.onmessage = function(event) {
      var callback = callbacks.pop();
      if (callback) {
        callback(asyncObject(parseMsg(event.data)).returnObject);
      }
    }
    websocket.onerror = function(event) {
      throw new Error(event.data);
    }

    __BATCH_SERVICE__ = {
      execute: function(script, data, callback) {
        callbacks.splice(0,0,callback); // queues callback
        function try_send() {
          if (websocket.readyState != WebSocket.OPEN) {
            setTimeout(try_send, 10);
          } else {
            websocket.send(script);
          }
        }
        try_send();
      }
    }

  };
})(window.onload);
