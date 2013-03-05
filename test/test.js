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

batch function f(remote x) {
  console.log(x.foo(XX.get()));
}

XX = {get: function() { return 1; } }
batch (var root in __BATCH_SERVICE__) {
  batch f(root.bar(20));
  console.log("hello");
}

    //batch (var root in __BATCH_SERVICE__) {
    //  page.putText("FOO(" + x + ") = " + root.foo(x));
    //}

    //batch function markedNameBySize(afile) {
    //  var name = afile.getName();
    //  if (afile.length() > 1000) {
    //    return "* " + name;
    //  } else {
    //    return "- " + name;
    //  }
    //}
    //var XX = {
    //  foo: function() {
    //    return 1000;
    //  }
    //}

    //batch function gt10(x) {
    //  return x > 10;
    //}

    //batch (var root in __BATCH_SERVICE__) {
    //  page.putText(
    //    "First pow of 2 > 10 = "
    //    + root.firstPow2That(function(x) { return x > 10; }) // gt10
    //  );
    //  page.putText("Directory: " + root.getDir().getName());
    //  for each (var file in root.getDir().listFiles()) {
    //    page.putText(root.foo(XX.foo()));
    //    page.putText(batch markedNameBySize(file));
    //    if (file.isDirectory()) {
    //      for each (var inner in file.listFiles()) {
    //        page.putText("-" + batch markedNameBySize(inner));
    //        if (inner.isDirectory()) {
    //          for each (var inner2 in inner.listFiles()) {
    //            page.putText("--" + batch markedNameBySize(inner2));
    //          }
    //        }
    //      }
    //    }
    //  }
    //  page.putText("EOD");
    //}
  };
})(window.onload);
