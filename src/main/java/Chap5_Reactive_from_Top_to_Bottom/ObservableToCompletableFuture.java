package Chap5_Reactive_from_Top_to_Bottom;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import rx.Observable;

/**
 * Created by chrisge on 9/11/17.
 */
public class ObservableToCompletableFuture {


  static <T> CompletableFuture<T> toFuture(Observable<T> observable) {

    CompletableFuture<T> promise = new CompletableFuture<>();
    observable.single().subscribe(promise::complete, promise::completeExceptionally);
    return promise;

  }
  static <T> CompletableFuture<List<T>> toFutureList(Observable<T> observable) {
    return toFuture(observable.toList());
  }

}
