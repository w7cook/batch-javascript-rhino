batch (var root in service) {
  console.log("Directory: " + root.getDir().getName());
  for each (var file in root.getDir().listFiles()) {
    var name = file.getName();
    if (file.length() > 1000) {
      console.log("* " + name);
    } else {
      console.log("- " + name);
    }
  }
}
