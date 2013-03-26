import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;

import batch.EvalService;
import batch.json.JSONTransport;
import batch.tcp.TCPClient;
import batch.syntax.Format;
import batch.util.BatchTransport;

public class TestWebServerWithTCP {

  public static void main(String[] argv) {
    int port = Integer.parseInt(argv[0]);
    String tcpServer = argv[1];
    int tcpPort = Integer.parseInt(argv[2]);

    System.out.println("Starting web socket server on port "+port);
    try {
      JSONTransport transport = new JSONTransport();
      BatchWebSocketServer server =
        runServer(port, tcpServer, tcpPort, transport);
      System.out.println("Enter anything to quit");
      System.in.read();
      server.stop();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static BatchWebSocketServer runServer(
      int port,
      String tcpServer,
      int tcpPort,
      BatchTransport transport)
      throws IOException {
    TCPClient<Object> service = new TCPClient<Object>(
      InetAddress.getByName(tcpServer), tcpPort, transport
    );
    BatchWebSocketServer server =
      new BatchWebSocketServer<String, Object>(
        new InetSocketAddress(port), service, transport, new Format()
      );
    server.start();
    return server;
  }

}
