import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;

import batch.Service;
import batch.syntax.Parser;
import batch.util.BatchFactory;
import batch.util.BatchTransport;
import batch.util.Forest;
import batch.util.ForestWriter;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class BatchWebSocketServer<E, T> extends WebSocketServer {

  Service<E, T> service;
  BatchTransport transport;
  BatchFactory<E> factory;

  public BatchWebSocketServer(
      InetSocketAddress address,
      Service<E, T> service,
      BatchTransport transport,
      BatchFactory<E> factory) throws IOException {
    super(address);
    this.service = service;
    this.transport = transport;
    this.factory = factory;
  }

  @Override 
  public void onOpen(WebSocket socket, ClientHandshake handshake) {
    System.out.println(
      "Connected to: " +
      socket.getRemoteSocketAddress().getAddress().getHostAddress()
    );
  }

  @Override
  public void onClose(
      WebSocket socket,
      int code,
      String reason,
      boolean remote) {
  }

  @Override
  public void onMessage(final WebSocket _socket, String message) {
    try {
      String[] parts = message.split("\n", 2);
      if (parts.length < 2) {
        System.out.println("No id before script: "+message);
        return;
      }
      final String _id = parts[0];
      String script = parts[1];
      Writer out = new Writer() {
        public void write(char[] cbuf, int off, int len) {
          String result = new String(cbuf, off, len);
          System.out.println("SENDING");
          System.out.println(result);
          _socket.send(_id + "\n" + result);
          //try{Thread.sleep(100);}catch(Exception e){}
        }
        public void close() {}
        public void flush() {}
      };
      ForestWriter forestWriter = transport.writer(out);
      service.executeServer(
        Parser.parse(script, factory),
        null, // TODO: deal with input forest
        forestWriter
      );
      forestWriter.complete(); // TODO Question: should this call complete outside of executeServer?
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onError(WebSocket socket, Exception e) {
    e.printStackTrace();
  }
}
