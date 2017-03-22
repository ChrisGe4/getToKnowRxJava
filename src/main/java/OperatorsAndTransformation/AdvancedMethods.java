package OperatorsAndTransformation;

import rx.Observable;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.Observable.interval;

/**
 * @author Chris.Ge
 */
public class AdvancedMethods {

    public static void main(String[] args) {
        //When Streams Are Not Synchronized with One Another: combineLatest(), withLatestFrom(), and amb()

        Observable<Long> red = Observable.interval(10, TimeUnit.MILLISECONDS);
        Observable<Long> green = Observable.interval(10, TimeUnit.MILLISECONDS);
        /**
         * The timestamp() operator wraps whatever the event type T was with rx.schedulers.Timestamped<T> class having two attributes: original value of type T and long timestamp when it was created.
         */

        Observable.zip(red.timestamp(), green.timestamp(),
            (r, g) -> r.getTimestampMillis() - g.getTimestampMillis()).forEach(System.out::println);


        /*
        F0:S0
    F1:S0
F2:S0
F2:S1
F3:S1
F4:S1
F4:S2
F5:S2
F5:S3

         */
        Observable.combineLatest(interval(17, MILLISECONDS).map(x -> "S" + x),
            interval(10, MILLISECONDS).map(x -> "F" + x), (s, f) -> f + ":" + s)
            .forEach(System.out::println);

        /*
        S0:F1
S1:F2
S2:F4
S3:F5
S4:F7
S5:F9
S6:F11
         */

        Observable<String> fast = interval(10, MILLISECONDS).map(x -> "F" + x);
        Observable<String> slow = interval(17, MILLISECONDS).map(x -> "S" + x);
        slow.withLatestFrom(fast, (s, f) -> s + ":" + f).forEach(System.out::println);
/*
This is by design, but if you truly need to preserve all events from the primary stream,
you must ensure that the other stream emits some dummy event as soon as possible


S0:FX
S1:FX
S2:FX
S3:FX
S4:FX
S5:FX
S6:F1
S7:F3
S8:F4
S9:F6
 */
        Observable<String> fast1 =
            interval(10, MILLISECONDS).map(x -> "F" + x).delay(100, MILLISECONDS).startWith("FX");
        Observable<String> slow1 = interval(17, MILLISECONDS).map(x -> "S" + x);
        slow1.withLatestFrom(fast1, (s, f) -> s + ":" + f).forEach(System.out::println);
        //For example, the following code block yields 0, 1 and 2
        Observable.just(1, 2).startWith(0).subscribe(System.out::println);

        //amb()  first emitted stream will win

        /*
        14:46:13.334: Subscribe to S
14:46:13.341: Subscribe to F
14:46:13.439: Unsubscribe from F
14:46:13.442: S0
14:46:13.456: S1
14:46:13.473: S2
14:46:13.490: S3
14:46:13.507: S4
14:46:13.525: S5
         */
        Observable.amb(stream(100, 17, "S"), stream(200, 10, "F")).subscribe(System.out::println);
        //or
        stream(100, 17, "S").ambWith(stream(200, 10, "F")).subscribe(System.out::println);


        /**scan  use the last generated value*/
        //factorials will generate 1, 2, 6, 24, 120, 720…, and so forth.
        //The rule of thumb is that the type of resulting Observable is always the same as the type of accumulator.
        Observable<BigInteger> factorials = Observable.range(2, 100)
            .scan(BigInteger.ONE, (big, cur) -> big.multiply(BigInteger.valueOf(cur)));


        /**reduce*/
        //if your sequence is infinite, scan() keeps emitting events for each upstream event, whereas reduce() will never emit any event.

    }

    static Observable<String> stream(int initialDelay, int interval, String name) {
        return Observable.interval(initialDelay, interval, MILLISECONDS).map(x -> name + x)
            .doOnSubscribe(() -> System.out.println("Subscribe to " + name))
            .doOnUnsubscribe(() -> System.out.println("Unsubscribe from " + name));



    }
}
