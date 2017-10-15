package Chap5_Reactive_from_Top_to_Bottom;

import java.io.IOException;
import java.time.Instant;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import rx.Single;
import rx.SingleSubscriber;
import rx.schedulers.Schedulers;

/**
 * Created by chrisge on 9/11/17.
 */
public class RxSingle {

  /*
  Single
is typically used for APIs known to return a single value (duh!) asynchronously and
with high probability of failure. Obviously, Single is a great candidate for request–
response types of communication involving I/O, like a network call
   */

  /*
  Technically, it is also possible to have a Single
that never completes, but multiple onSuccess() invocations are not allowed.
   */
  static AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();


  public void run() {
    Single<String> single = Single.just("hello world");
    single.subscribe(System.out::println);
    Single<Instant> error = Single.error(new RuntimeException("Opps!"));
    error.observeOn(Schedulers.io()).subscribe(System.out::println, Throwable::printStackTrace);

    Single<String> example = fetch("http://www.example.com").flatMap(this::body2);


  }

  static Single<Response> fetch(String address) {

    return Single.create(sub -> asyncHttpClient.prepareGet(address).execute(handler(sub)));

  }

  static AsyncCompletionHandler handler(SingleSubscriber<? super Response> subscriber) {
    return new AsyncCompletionHandler() {
      @Override
      public Response onCompleted(Response response) throws Exception {
        subscriber.onSuccess(response);
        return response;
      }

      @Override
      public void onThrowable(Throwable t) {
        subscriber.onError(t);
      }
    };
  }


  Single<String> body(Response response) {
    return Single.create(sub -> {
      try {
        sub.onSuccess(response.getResponseBody());
      } catch (IOException e) {
        sub.onError(e);
      }

    });
  }

   Single<String> body2(Response response) {

    return Single.fromCallable(() -> response.getResponseBody());
  }

}
