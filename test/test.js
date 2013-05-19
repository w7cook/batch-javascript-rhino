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

//batch function f(remote x, remote i, local y) {
//  console.log(x.foo(i) + y + i);
//}
batch function local getSomeBigFileName(remote dir, remote i) {
  for each (var file in dir.listFiles()) {
    if (file.length() > i) {
      return file.getName();
    }
  }
  return "<NONE>";
}
var global = {test: function(x) { return !!x; }}
batch function local testingIfs(remote r) {
  // local
  if (global.test(r.length())) {
    return r.getName();
  }
  return '';
}
batch function remote f(remote x, remote i) {
  return (
    x.foo(i) > 10
      ? i
      : "BAD"
  );
}
batch function remote g(remote x, remote i) {
  if (!!x.foo(i)) {
    return i || 10 && 22;
  } else {
    return "BAD"
  };
}

XX = {bigSize: function() { return 1000; } }
batch __BATCH_SERVICE__.execute(function(root) {
  page.putText(
    "Following file is big: "
    + batch getSomeBigFileName(root.getDir(),batch f(root,XX.bigSize()))
  );
  //batch f(root.bar(20), XX.get(), "yay");
  page.putText("hello");
});

    //batch __BATCH_SERVICE__.execute(function(root) {
    //  page.putText("FOO(" + x + ") = " + root.foo(x));
    //});

    batch function remote markedNameBySize(remote afile) {
      var name = afile.getName();
      if (afile.length() > 1000) {
        return "* " + name;
      } else {
        return "- " + name;
      }
    }
    var XX = {
      foo: function() {
        return 1000;
      }
    }

    batch function remote gt100(remote x) {
      return x > 100;
    }

    batch __BATCH_SERVICE__.execute(function(root) {
      page.putText(
        "First pow of 2 > 100 = "
        + root.firstPow2That(batch gt100)
      );
      page.putText(root.foo(XX.foo()));
      page.putText("Directory: " + root.getDir().getName());
      for each (var file in root.getDir().listFiles()) {
        page.putText(batch markedNameBySize(file));
        if (file.isDirectory()) {
          for each (var inner in file.listFiles()) {
            page.putText("-" + batch markedNameBySize(inner));
            if (inner.isDirectory()) {
              for each (var inner2 in inner.listFiles()) {
                page.putText("--" + batch markedNameBySize(inner2));
              }
            }
          }
        }
      }
      page.putText("EOD");
    });
  };
})(window.onload);
