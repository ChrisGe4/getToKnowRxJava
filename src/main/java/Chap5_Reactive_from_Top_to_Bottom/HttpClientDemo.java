package Chap5_Reactive_from_Top_to_Bottom;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import java.net.URL;
import rx.Observable;

/**
 * Created by chrisge on 9/8/17.
 */
public class HttpClientDemo {

  public static void main(String[] args) {
    /*
    ByteBuf is
an abstraction over a chunk of data received over the wire. From the client perspec‐
tive, this is part of the response. That is right, RxNetty goes a bit further compared to
other nonblocking HTTP clients and does not simply notify us when the entire
response arrives. Instead, we get a stream of ByteBuf messages, optionally followed
by Observable completion when the server decides to drop the connection.
     */
    Observable<ByteBuf> response = HttpClient
        .newClient("example.com", 80)
        .createGet("/")
        .flatMap(HttpClientResponse::getContent);
    response
        .map(bb -> bb.toString(UTF_8))
        .subscribe(System.out::println);

    //If you want a steady stream of packets flowing
    //through all of these sources
    Observable<URL> sources = Observable.just();
    Observable<ByteBuf> packets =
        sources
            .flatMap(url -> HttpClient
                .newClient(url.getHost(), url.getPort())
                .createGet(url.getPath()))
            .flatMap(HttpClientResponse::getContent);

    /*
    If you want to
first transform incoming data, perhaps by combining chunks of data into a single
event—you can do this easily, for example with reduce() . Here is the upshot: you can
easily have tens of thousands of open HTTP connections, idle or receiving data. The
limiting factor is no longer memory, but the processing power of your CPU and net‐
work bandwidth. JVM does not need to consume gigabytes of memory to process a
reasonable number of transactions.
     */

  }
}
