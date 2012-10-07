var s = '';
batch(var dir in dirServer) {
  for each (var file in dir.getFiles()) {
    s = s + file.getName() + ', ';
  }
}
console.log(s);
