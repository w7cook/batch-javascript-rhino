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

    var websocket = new WebSocket('ws://localhost:9999');
    var callbacks = [];
    websocket.onmessage = function(event) {
      var callback = callbacks.pop();
      if (callback) {
        callback(parseMsg(event.data));
      }
    }
    websocket.onerror = function(event) {
      throw new Error(event.data);
    }

    __BATCH_SERVICE__ = {
      execute: function(script, data, callback) {
        callbacks.splice(0,0,callback); // queues callback
        function try_send() {
          if (!websocket.readyState) {
            setTimeout(try_send, 10);
          } else if (!websocket.send(script)) {
            throw new Error('Failed to send script to server');
          }
        }
        try_send();
      }
    }

  };
})(window.onload);
