package Chap5_Reactive_from_Top_to_Bottom;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import rx.Observable;

/**
 * @author Chris.Ge
 */
public class EurUsdCurrencyTcpServer {

  private static final BigDecimal RATE = new BigDecimal("1.06448");


  public static void main(String[] args) {
    TcpServer
        .newServer(8080)
        .<String, String>pipelineConfigurator(pipeline -> {
          pipeline.addLast(new LineBasedFrameDecoder(1024));
          pipeline.addLast(new StringDecoder(UTF_8));
        }).start(connection -> {

      Observable<String> output = connection.getInput().map(BigDecimal::new)
          .flatMap(eur -> eurToUsd(eur));
      return connection.writeAndFlushOnEach(output);
    }).awaitShutdown();
  }

  static Observable<String> eurToUsd(BigDecimal eur) {
    return Observable.just(eur.multiply(RATE)).map(amount -> eur + " EUR is " + amount + " USD\n")
        .delay(1, TimeUnit.SECONDS);

  }
}
