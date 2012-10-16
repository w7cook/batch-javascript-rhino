batch (var root in service) {
  console.log("Directory: " + root.getDir().getName());
  for each (var file in root.getDir().listFiles()) {
    console.log("^ " + file.getName());
  }
}
