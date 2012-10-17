importClass(Packages.batch.tcp.TCPClient);
importClass(Packages.batch.json.JSONTransport);
importPackage(Packages.batch.util);
importClass(java.net.InetAddress);

var service = new TCPClient(
  InetAddress.getByName('localhost'),
  9999,
  new JSONTransport()
);

var console = {
  log: print
};

load('bin/js/compiled-test.js');
