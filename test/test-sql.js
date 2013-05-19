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

batch function remote byProductName(remote product) {
  return product.ProductName;
}

batch __BATCH_SERVICE__.execute(function(db) {
  for each (var product in db.Products.orderBy(batch byProductName)) {
    if (batch isSoldOut(product)) {
      page.putText(product.ProductName + " is sold out!");
    }
  }
  page.putText("--all--");
  for each (var p2 in db.Products.orderBy(function(x){return x.ProductName;})) {
    page.putText(p2.ProductName);
  }
});
  };
})(window.onload);
