package Chap5_Reactive_from_Top_to_Bottom;

import java.util.concurrent.CompletableFuture;
import rx.Observable;

/**
 * Created by chrisge on 9/8/17.
 */
public class CompletableFutureAndObservable {
/*
call applyToEither() on
the first two Future s and then take the result (fastest out of the first two) and apply it
with the third upstream Future (fastest out of the first three). By iteratively calling
applyToEither() , we get the CompletableFuture representing the fastest overall.
This handy trick can be efficiently implemented using the reduce() operator.
 */

/*
But remember that just like map() is called thenApply() in Future s, flatMap() is called thenCompose() :
 */

  /**
   * CompletableFuture is hot and cached
   */
  static class Util {

    static <T> Observable<T> observe(CompletableFuture<T> future) {

      return Observable.create(subscriber -> {
         //Don't do this!
       // We can create many Observable s based on one CompletableFu
       // ture , and every Observable can have multiple Subscriber s.
//        subscriber.add(Subscriptions.create(
//            () -> future.cancel(true)));
        future.whenComplete((value, exception) -> {
          if (exception != null) {
            subscriber.onError(exception);
          } else {
            subscriber.onNext(value);
            subscriber.onCompleted();
          }
        });
      });

    }

  }

}
