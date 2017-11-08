package Chap6;


import static java.util.concurrent.TimeUnit.SECONDS;
import static rx.Observable.just;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import rx.Observable;

/**
 * Created by chrisge on 11/6/17.
 */
public class SamplesAndThrottling {

  public static void main(String[] args) {
    /*
    The sample() operator looks at the upstream Observable periodically (for example, every second) and
emits the last encountered event. If there were no event at all in the last one-second
period, no sample is forwarded downstream and the next sample will be taken after
one second, as illustrated in this sample:
     */
/*
The preceding code snippet will print something similar to the following:
1088ms:141
2089ms:284
3090ms:427
4084ms:569
5085ms:712

 */
    long startTime = System.currentTimeMillis();
    Observable.interval(7, TimeUnit.MILLISECONDS).timestamp().sample(1, SECONDS)
        .map(ts -> ts.getTimestampMillis() - startTime + "ms: " + ts.getValue()).take(5)
        .subscribe(System.out::println);

/*
list of
names that appear with some absolute delays

Mary appears after 100 milliseconds
from subscription, whereas Dorothy appears after 4.8 seconds. The sample() operator
will periodically (every second) pick the last seen name from the stream within the
last period. So, after the first second, we println Linda, followed by Barbara a second
later. Now between 2,000 and 3,000 milliseconds since subscription, no name
appeared, so sample() does not emit anything. Two seconds after Barbara was emit‐
ted, we see Susan. sample() will forward completion (and errors, as well) discarding
the last period.

 */

    Observable<String> names = just("Mary", "Patricia", "Linda",
        "Barbara",
        "Elizabeth", "Jennifer", "Maria", "Susan",
        "Margaret", "Dorothy");

    Observable<Long> absoluteDelayMillis = just(0.1, 0.6, 0.9, 1.1,
        3.3, 3.4, 3.5, 3.6,
        4.4, 4.8).map(d -> (long) (d * 1_000));

    Observable<String> delayedNames = names
        .zipWith(absoluteDelayMillis, (n, d) -> just(n).delay(d, TimeUnit.MILLISECONDS))
        .flatMap(o -> o);

    delayedNames.sample(1, SECONDS).subscribe(System.out::println);


    /*
    If we want to see Dorothy appearing as well, we can artificially postpone the completion notification, as is done here:
     */

    delayedNames.concatWith(delayCompletion()).sample(1, SECONDS)
        .subscribe(System.out::println);

    //equivalent:
    delayedNames.sample(1, SECONDS);
    delayedNames.sample(Observable.interval(1, SECONDS));

    /*
    The output looks like this:
Mary
Barbara
Elizabeth
Margaret

Just like sample() (aka throttleLast() ), throttleFirst() does not emit any event
when no new name appeared between Barbara and Elizabeth.
     */
    delayedNames.throttleFirst(1, SECONDS).subscribe(System.out::println);

    /**buffer()*/
    Observable.range(1, 7).buffer(3).subscribe((List<Integer> list) -> System.out.println(list));
    /*work in batch*/
    delayedNames.subscribe(Repository::store);
    //vs
    delayedNames.buffer(5).subscribe(Repository::storeAll);

    /*moving window of a certain size:
    * This yields several overlapping lists:
[1,2, 7]
[2,3, 3]
[3,4, 4]
[4,5, 5]
[5,6, 6]
[6,   7]
[7]
    *
    * */
    Observable.range(1, 7).buffer(3, 1).subscribe(System.out::println);


    /*
    *
    * -0.048286450121296814
-0.042169613014985986
-0.03818850454846762
-0.01914760892232216
-0.018235127648492073
-0.013019198206939763
-0.012708818881983908
-0.004368767885449557
0.006379274899815462
0.02083077417055593
0.02162543326900024
    *
    * */

    Random random = new Random();
    Observable.defer(() -> just(random.nextGaussian())).repeat(1000).buffer(100, 1).map(
        SamplesAndThrottling::averageOfList).subscribe(System.out::println);

    /*
     *output is: [1] [3] [5] [7] .
     */
    Observable<List<Integer>> odd = Observable
        .range(1, 7)
        .buffer(1, 2);
    odd.subscribe(System.out::println);
//to one obs

    Observable<Integer> oddd = Observable
        .range(1, 7)
        .buffer(1, 2).flatMapIterable(list -> list);
    /*aggregates all upstream events within that period.

[Mary, Patricia, Linda]
[Barbara]
[]
[Elizabeth, Jennifer, Maria, Susan]
[Margaret, Dorothy]

     */
    delayedNames.buffer(1, SECONDS).subscribe(System.out::println);

    /** case study
     During business hours (9:00–17:00), we take 100-millisecond long snapshots
     every second (processing approximately 10% of data)
     • Outside business hours we look only at 200-millisecond long snapshots taken
     every 5 seconds (4%)

     */

  }

  static <T> Observable<T> delayCompletion() {
    return Observable.<T>empty().delay(1, SECONDS);
  }

  private static double averageOfList(List<Double> list) {
    return list
        .stream().collect(Collectors.averagingDouble(x -> x));
  }

  static interface Repository {

    void store(String record);

    void storeAll(List<String> records);
  }
}
