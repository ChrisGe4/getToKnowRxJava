package OperatorsAndTransformation;

import rx.Observable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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


        /**reduce
         public <R> Observable<R> reduce(R initialValue,Func2<R,T,R> accumulator){
         return scan(initialValue,accumulator).takeLast(1);
         }
         */

        //if your sequence is infinite, scan() keeps emitting events for each upstream event, whereas reduce() will never emit any event.
        //eg. Imagine that you have a source of CashTransfer objects with getAmount() method returning BigDecimal. We would like to calculate the total amount on all transfers. The following two transformations are equivalent. They iterate over all transfers and add up amounts, beginning at ZERO:
        Observable<CashTransfer> transfers = Observable.just(new CashTransfer());

        Observable<BigDecimal> total1 = transfers.reduce(BigDecimal.ZERO,
            (totalSoFar, transfer) -> totalSoFar.add(transfer.getAmount()));
        //prefer smaller, more composable transformations over a single big one
        Observable<BigDecimal> total2 =
            transfers.map(CashTransfer::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        /**
         * collect
         **both reduce() and collect() are nonblocking,
         */
        // if use reduce
        Observable<List<Integer>> all =
            Observable.range(10, 20).reduce(new ArrayList<>(), (list, item) -> {
                list.add(item);
                return list;
            });
        //better
        Observable<List<Integer>> all1 =
            Observable.range(10, 20).collect(ArrayList::new, List::add);

        /**Asserting Observable Has Exactly One Item Using single()
         * In case this assumption is wrong, you will receive an exception
         * */

        /**Dropping Duplicates Using distinct() and distinctUntilChanged()
         *
         * Be sure to remember that distinct() must keep in mind all events/keys seen so far for eternity
         *
         * In practice, distinctUntilChanged() is often more reasonable
         * */

        Observable<Integer> randomInts = Observable.create(subscriber -> {
            Random random = new Random();
            while (!subscriber.isUnsubscribed()) {
                subscriber.onNext(random.nextInt(1000));
            }
        });

        Observable<Integer> uniqueRandomInts = randomInts.distinct().take(10);

        //with predicate
        //        Observable<Status> distinctUserIds = tweets
        //            .distinct(status -> status.getUser().getId());



        //The important difference between distinct() and distinctUntilChanged() is that the latter can produce duplicates but only if they were separated by a different value.
        //distinctUntilChanged() must only remember the last seen value. distinctUntilChanged() has a predictable, constant memory footprint, as opposed to distinct().

        Observable<Weather> tempChanges =
            Observable.just(new Weather()).distinctUntilChanged(Weather::getTemperature);

        /**take(n) and skip(n)
         *
         *
         * Observable beginning with event n+1. Both operators are quite liberal: negative numbers are treated as zero, exceeding the Observable size is not treated as a bug:
         * */

        Observable.range(1, 5).take(3);  // [1, 2, 3]
        Observable.range(1, 5).skip(3);  // [4, 5]
        Observable.range(1, 5).skip(5);  // []

        /**takeLast(n) and skipLast(n)
         *
         *keep a buffer of the last n
         *
         * It makes no sense to call takeLast() on an infinite stream because it will never emit anything
         * */

        Observable.range(1, 5).takeLast(2);  // [4, 5]
        Observable.range(1, 5).skipLast(2);  // [1, 2, 3]

        /**first() and last()
         *
         * overloaded versions that take predicates
         * */
        //can be implement via
        Observable.range(1, 5).take(1).single();

        /**takeFirst(predicate)
         *
         *filter(predicate).take(1)
         *
         *
         * it will not break with NoSuchElementException
         * */


        /**takeUntil(predicate) and takeWhile(predicate)
         *
         *So the only difference is that takeUntil() will emit the first nonmatching value, whereas takeWhile() will not.
         * */
        Observable.range(1, 5).takeUntil(x -> x == 3);  // [1, 2, 3]
        Observable.range(1, 5).takeWhile(x -> x != 3);  // [1, 2]


        /**elementAt(n), elementAtOrDefault(), firstOrDefault(), lastOrDefault(), and singleOrDefault().*/


        /**count()
         *
         * all operators are lazy so this will work even for quite large streams.
         *
         * */
        //can be implement via
        Observable<Integer> size =
            Observable.just('A', 'B', 'C').reduce(0, (sizeForNow, ch) -> sizeForNow + 1);

        /**all(predicate), exists(predicate), and contains(value)*/

        Observable<Integer> numbers = Observable.range(1, 5);

        numbers.all(x -> x != 4);    // [false]
        numbers.exists(x -> x == 4); // [true]
        numbers.contains(4);         // [true]



    }



    //helper
    static Observable<String> stream(int initialDelay, int interval, String name) {
        return Observable.interval(initialDelay, interval, MILLISECONDS).map(x -> name + x)
            .doOnSubscribe(() -> System.out.println("Subscribe to " + name))
            .doOnUnsubscribe(() -> System.out.println("Unsubscribe from " + name));



    }

    private static class CashTransfer {
        public BigDecimal getAmount() {
            return null;
        }
    }


    private static class Weather {
        public static <U> U getTemperature(Weather weather) {

            return null;
        }
    }
}
