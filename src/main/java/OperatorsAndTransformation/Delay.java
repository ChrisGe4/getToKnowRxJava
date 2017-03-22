package OperatorsAndTransformation;

import java.util.concurrent.TimeUnit;

import static rx.Observable.just;
import static rx.Observable.timer;


/**
 * @author Chris.Ge
 */
public class Delay {
    // delay() is more comprehensive than timer() because it shifts every single event further by a given amount of time, whereas timer() simply “sleeps” and emits a special event after given time
    public static void main(String[] args) throws InterruptedException {
        just("Lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit")
            .delay(word -> timer(word.length(), TimeUnit.SECONDS)).subscribe(System.out::println);

        TimeUnit.SECONDS.sleep(15);

        //The preceding examples reveals an interesting characteristic of flatMap(): it does not preserve the original order of events
        just("Lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit")
            .flatMap(word -> timer(word.length(), TimeUnit.SECONDS).map(x -> word));

        //In this example, we delay event 10L by 10 seconds and event 1L (chronologically appearing later in upstream) by 1 second. As a result, we see 1 after a second and 10 nine seconds later—the order of events in upstream and downstream is different
        just(10L, 1L).flatMap(x -> just(x).delay(x, TimeUnit.SECONDS))
            .subscribe(System.out::println);


    }


}
