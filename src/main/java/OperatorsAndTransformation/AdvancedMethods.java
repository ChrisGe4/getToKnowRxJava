package OperatorsAndTransformation;

import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.observables.GroupedObservable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static rx.Observable.interval;
import static rx.Observable.just;

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
        just(1, 2).startWith(0).subscribe(System.out::println);

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
        Observable<CashTransfer> transfers = just(new CashTransfer());

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
            just(new Weather()).distinctUntilChanged(Weather::getTemperature);

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
            just('A', 'B', 'C').reduce(0, (sizeForNow, ch) -> sizeForNow + 1);

        /**all(predicate), exists(predicate), and contains(value)*/

        Observable<Integer> numbers = Observable.range(1, 5);

        numbers.all(x -> x != 4);    // [false]
        numbers.exists(x -> x == 4); // [true]
        numbers.contains(4);         // [true]


        /**Ways of Combining Streams: concat(), merge(), and switchOnNext()*/

        /**concat() (and instance method concatWith()) allow joining together two Observables: when the first one completes, concat() subscribes to the second one
         //ref: ordering

         like all operators we met so far, concat() is nonblocking, it emits events only when the underlying stream emits something
         */

        //receive only the first few and last few items from a very long stream
        Observable<Integer> veryLong = just(1, 2, 3, 4);
        final Observable<Integer> ends = Observable.concat(veryLong.take(5), veryLong.takeLast(5));
        // fallback value when first stream did not emit anything
        //initially subscribe to fromCache and if that emits one item, concat() will not subscribe to fromDb.
        Observable<Car> fromCache = loadFromCache();
        Observable<Car> fromDb = loadFromDb();

        Observable<Car> found = Observable.concat(fromCache, fromDb).first();

        /**
         * What we want is to have the first word appear immediately and then the second word after a delay, depending on the length of the first word
         */

        Observable<String> alice = speak("To be, or not to be: that is the question", 110);
        Observable<String> bob = speak("Though this be madness, yet there is method in't", 90);
        Observable<String> jane = speak("There are more things in Heaven and Earth, "
            + "Horatio, than are dreamt of in your philosophy", 100);

        //Merge
        /*
        Alice: To
Bob:   Though
Jane:  There
Alice: be
Alice: or
Jane:  are
Alice: not
Bob:   this
Jane:  more
Alice: to
Jane:  things
Alice: be
Bob:   be
Alice: that
Bob:   madness
Jane:  in
Alice: is
Jane:  Heaven
Alice: the
Bob:   yet
Alice: question
Jane:  and
Bob:   there
Jane:  Earth

         */

        Observable.merge(alice.map(w -> "Alice: " + w), bob.map(w -> "Bob:   " + w),
            jane.map(w -> "Jane:  " + w)).subscribe(System.out::println);

        /* Concat
        Alice: To
Alice: be
Alice: or
Alice: not
Alice: to
Alice: be
Alice: that
Alice: is
Alice: the
Alice: question
Bob:   Though
Bob:   this
Bob:   be
Bob:   madness
Bob:   yet
Bob:   there
Bob:   is
Bob:   method
Bob:   in't
Jane:  There
Jane:  are
....
         */
        Observable.concat(alice.map(w -> "Alice: " + w), bob.map(w -> "Bob:   " + w),
            jane.map(w -> "Jane:  " + w)).subscribe(System.out::println);


        /**switchOnNext
         *
         *
         *In other words, when we have a stream of streams, switchOnNext() always forwards downstream events from the last inner stream, even if older streams keep forwarding fresh events.
         * */
 /*
        //A - observable appears immediately in the outer stream but begins emitting events with some delay
        map(innerObs -> innerObs.delay(rnd.nextInt(5), SECONDS))

        //B - we shift the entire Observable event forward in time so that it appears in the outer Observable much later.
        flatMap(innerObs -> just(innerObs).delay(rnd.nextInt(5), SECONDS))
        */


 /*
    One of the possible outcomes, due to the random nature of this exampl
Jane:  There
Jane:  are
Jane:  more
Alice: To
Alice: be
Alice: or
Alice: not
Alice: to
Bob:   Though
Bob:   this
Bob:   be
Bob:   madness
Bob:   yet
Bob:   there
Bob:   is
Bob:   method
Bob:   in't

  */

 /*
 Theoretically, switchOnNext() could produce all of the events from the inner Observables if they did not overlap, completing before the next one appears.
  */
        Random rnd = new Random();
        Observable<Observable<String>> quotes =
            just(alice.map(w -> "Alice: " + w), bob.map(w -> "Bob:   " + w),
                jane.map(w -> "Jane:  " + w))

                .flatMap(innerObs -> just(innerObs)
                    //delay happens to the content of obs, if want to delay an obs, make it obs<obs>
                    .delay(rnd.nextInt(5), SECONDS));

        Observable.switchOnNext(quotes).subscribe(System.out::println);
        /** groupBy()
         *
         * use case:
         * data is not stored as a snapshot of current state and mutated in place; that is, using SQL UPDATE queries. Instead, a sequence of immutable domain events (facts) about events that already happened are kept in an append-only data store. Using this design, we never overwrite any data, effectively having an audit log for free.
         *
         *we quickly see a possible race-condition: two threads can consume different events, modify the same Reservation and try to store it—but the first update is overwritten and effectively lost. Technically you can try optimistic locking, but another problem remains: the order of facts is no longer guaranteed. This is not a problem when two unrelated Reservation instances (with different UUID) are touched. But applying facts on the same Reservation object in a different order from which they actually occurred can be disastrous.
         * */
        FactStore factStore = new CassandraFactStore();

        Observable<ReservationEvent> facts = factStore.observe();
        //---from

        facts.flatMap(event -> updateProjectionAsync(event)).subscribe();

        //...


        //---to
        Observable<GroupedObservable<UUID, ReservationEvent>> grouped =
            facts.groupBy(ReservationEvent::getReservationUuid);

        grouped.subscribe(byUuid -> {
            byUuid.subscribe(event -> updateProjection(event));
        });

        /** compose()
         *
         * when you can no longer fluently chain operators; in other words, you cannot say: obs.op1().odd().op2().
         *so  make a change to odd and use it with compose
         * Notice that the odd() function is executed eagerly when Observable is assembled
         *
         * */
        //[A, B, C, D, E...]
        Observable<Character> alphabet =
            Observable.range(0, 'Z' - 'A' + 1).map(c -> (char) ('A' + c));
        alphabet.compose(odd()).forEach(System.out::println);

        /**lift and buffer*/
    }


    //every odd element
    //new

    static <T> Observable.Transformer<T, T> odd() {
        Observable<Boolean> trueFalse = just(true, false).repeat();
        //Interestingly, if you want to emit even values (2nd, 4th, 6th, etc.) rather than odd (1st, 3rd, 5th, etc.), simply replace trueFalse with trueFalse.skip(1).
        return upstream -> upstream.zipWith(trueFalse, Pair::of).filter(Pair::getRight)
            .map(Pair::getLeft);
    }

    //old
    static <T> Observable<T> odd(Observable<T> upstream) {
        Observable<Boolean> trueFalse = just(true, false).repeat();
        return upstream.zipWith(trueFalse, Pair::of).filter(Pair::getRight).map(Pair::getLeft);

        // or without pair
        // upstream.zipWith(trueFalse, (t, bool) -> bool ? just(t) : empty()).flatMap(obs -> obs);
    }


    static Observable<String> speak(String quote, long millisPerChar) {
        /*
         * (Though, 0)
            (this, 600)
            (be, 1000)
            (madness, 1200)
         */
        String[] tokens = quote.replaceAll("[:,]", "").split(" ");
        Observable<String> words = Observable.from(tokens);
        Observable<Long> absoluteDelay = words.map(String::length).map(len -> len * millisPerChar)
            //Assuming millisPerChar is 100 and words are Though this be madness, we first get the following stream: 600, 400, 200, 700. If we were to simply delay() each word by that duration, "be" word would appear first and other words would be scrambled as well. What we really want is a cumulative sequence of absolute delays, like this: 600, 600 + 400 = 1,000; 1,000 + 200 = 1,200; 1,200 + 700 = 1,900. This is easy using the scan() operator
            .scan((total, current) -> total + current);
        //We do not want the first word to be delayed at all. Instead, the length of the first word should influence the delay of the second word, the total length of the first and second word should influence the delay of the third word, and so on
        return words.zipWith(absoluteDelay.startWith(0L), Pair::of)
            .flatMap(pair -> just(pair.getLeft()).delay(pair.getRight(), MILLISECONDS));

        /**
         *
         * Incorrect. The Observable will first emit all one-letter words at the same time. Then, after a while, all two-letter words followed by all three-letter words. What we want is to have the first word appear immediately and then the second word after a delay
         *
         words.flatMap(word -> Observable
         .just(word)
         .delay(word.length() * millisPerChar, MILLISECONDS));

         */
    }

    //helper
    static Observable<String> stream(int initialDelay, int interval, String name) {
        return Observable.interval(initialDelay, interval, MILLISECONDS).map(x -> name + x)
            .doOnSubscribe(() -> System.out.println("Subscribe to " + name))
            .doOnUnsubscribe(() -> System.out.println("Unsubscribe from " + name));



    }

    static void updateProjection(ReservationEvent event) {
        //        UUID uuid = event.getReservationUuid();
        //        Reservation res = loadBy(uuid).orElseGet(() -> new Reservation(uuid));
        //        res.consume(event);
        //        store(event.getUuid(), res);
    }

    static Observable<ReservationEvent> updateProjectionAsync(ReservationEvent event) {
        //possibly asynchronous
        return null;
    }

    private static Optional<Reservation> loadBy(UUID uuid) {
        return null;
    }

    private static Observable<Car> loadFromDb() {
        return null;
    }

    private static Observable<Car> loadFromCache() {
        return null;
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


    private static class Car {
    }


    private static class ReservationEvent {
        public static <K> K getReservationUuid(ReservationEvent reservationEvent) {

            return null;
        }
    }


    private static class FactStore {
        public Observable<ReservationEvent> observe() {
            return null;
        }
    }


    private static class CassandraFactStore extends FactStore {
    }


    private static class Reservation {
        public Reservation(UUID uuid) {
        }
    }
}
