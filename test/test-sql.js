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

batch function remote isSoldOut(remote product) {
  return product.UnitsInStock < 1;
}

batch (var db in __BATCH_SERVICE__) {
  for each (var product in db.Products.orderBy(function(x){return x.ProductName;})) {
    if (batch isSoldOut(product)) {
      page.putText(product.ProductName + " is sold out!")
    }
  }
}
  };
})(window.onload);
