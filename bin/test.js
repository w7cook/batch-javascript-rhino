batch (var root in service) {
  console.log("Directory: " + root.getDir().getName());
  var i = 0;
  for each (var file in root.getDir().listFiles()) {
    console.log("^ " + i + ":" + file.getName());
    i = i + 1;
  }
}
