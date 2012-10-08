var s = '';
var i = 232;
batch(var dir in dirServer) {
  for each (var file in dir.getFiles()) {
    s = s + file.getPart(i) + ', ';
  }
}
console.log(s);
