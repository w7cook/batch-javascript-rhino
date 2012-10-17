import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;

import batch.Service;
import batch.syntax.BatchScriptParser;
import batch.syntax.Expression;
import batch.util.BatchFactory;
import batch.util.BatchTransport;
import batch.util.Forest;

import org.antlr.runtime.RecognitionException;

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
  public synchronized void onMessage(final WebSocket _socket, String message) {
    try {
      Expression exp = BatchScriptParser.parse(message);
      Forest result = service.execute(exp.run(factory), null);
      StringWriter out = new StringWriter();
      transport.write(result, out);
      System.out.println("SENDING");
      System.out.println(out.toString());
      _socket.send(out.toString());
    } catch (RecognitionException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onError(WebSocket socket, Exception e) {
    e.printStackTrace();
  }
}
