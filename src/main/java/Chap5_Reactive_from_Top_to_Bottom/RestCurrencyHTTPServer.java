package Chap5_Reactive_from_Top_to_Bottom;

import io.reactivex.netty.protocol.http.server.HttpServer;
import java.math.BigDecimal;
import rx.Observable;

/**
 * usage:
 * curl -v localhost:8080/10.99
 *
 *
 * Created by chrisge on 9/8/17.
 */
public class RestCurrencyHTTPServer {

  private static final BigDecimal RATE = new BigDecimal("1.06448");

  public static void main(final String[] args) {

    HttpServer
        .newServer(8080)
        .start((req, resp) -> {
          String amountStr = req.getDecodedPath().substring(1);
          BigDecimal amount = new BigDecimal(amountStr);
          Observable<String> response = Observable
              .just(amount)
              .map(eur -> eur.multiply(RATE))
              .map(usd ->
                  "{\"EUR\": " + amount + ", " +
                      "\"USD\": " + usd + "}");
          return resp.writeString(response);
        })
        .awaitShutdown();





  }
}
