var page = {
  putText: function(text) {
    var div = document.getElementById("root");
    var elem = document.createElement('div');
    elem.appendChild(document.createTextNode(text));
    div.appendChild(elem);
  }
};
window.onload = (function(old_onload) {
  return function() {
    if (old_onload) old_onload.apply(window, arguments);

    batch (var root in __BATCH_SERVICE__) {
      page.putText("Directory: " + root.getDir().getName());
      for each (var file in root.getDir().listFiles()) {
        var name = file.getName();
        if (file.length() > 1000) {
          page.putText("* " + name);
        } else {
          page.putText("- " + name);
        }
      }
    }
  };
})(window.onload);
