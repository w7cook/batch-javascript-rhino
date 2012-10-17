import java.io.IOException;
import java.net.InetSocketAddress;

import batch.EvalService;
import batch.json.JSONTransport;
import batch.syntax.Evaluate;
import batch.util.BatchTransport;
import eval.BasicInterface;
import eval.BasicObj;

public class TestWebServer {

  public static void main(String[] argv) {
    int port = Integer.parseInt(argv[0]);

    try {
      runServer(port, new JSONTransport());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void runServer(int port, BatchTransport transport)
      throws IOException {
    BasicObj root = new BasicObj(1000);
    EvalService<BasicInterface> service = new EvalService<BasicInterface>(root);
    BatchWebSocketServer<Evaluate, BasicInterface> server =
      new BatchWebSocketServer<Evaluate, BasicInterface>(
        new InetSocketAddress(port), service, transport, new batch.syntax.Eval()
      );
    server.start();
  }

}
