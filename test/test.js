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
    var x = 1000;
      //var set = root.makeSet();
      //for each (var x in mySet) {
      //  set.add(x);
      //}
      //root.tryItOn(set);

    batch (var root in __BATCH_SERVICE__) {
      page.putText("FOO(" + x + ") = " + root.foo(x));
    }

    batch function test(x) {
      page.putText('Checked on ' + x);
      return x > 10;
    }

    batch (var root in __BATCH_SERVICE__) {
      page.putText(
        "First pow of 2 > 10 = "
        + root.firstPow2That(function(x) { return x > 10; })
      );
      page.putText("Directory: " + root.getDir().getName());
      for each (var file in root.getDir().listFiles()) {
        var name = file.getName();
        if (file.length() > 1000) {
          page.putText("* " + name);
        } else {
          page.putText("- " + name);
        }
        if (file.isDirectory()) {
          for each (var inner in file.listFiles()) {
            page.putText("-+ " + inner.getName());
            if (inner.isDirectory()) {
              for each (var inner2 in inner.listFiles()) {
                page.putText("--+ " + inner2.getName());
              }
            }
          }
        }
      }
      page.putText("EOD");
    }
  };
})(window.onload);