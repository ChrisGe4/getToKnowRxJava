package Chap4_applyingRx;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Chris.Ge
 */
public class Schedulers_example {

    static Observable<String> simple() {
        return Observable.create(subscriber -> {

            System.out.println(("Subscribed"));
            subscriber.onNext("A");
            subscriber.onNext("B");
            subscriber.onCompleted();
        });
    }

    public static void main(String[] args) {
        System.out.println("Starting");
        final Observable<String> obs = simple();
        System.out.println("Created");
        final Observable<String> obs2 = obs.map(x -> x).filter(x -> true);
        System.out.println("Transformed");
        obs2.subscribe(x -> System.out.println("Got " + x), Throwable::printStackTrace,
            () -> System.out.println("Completed"));
        System.out.println("Exiting");
    }

    public void run() {

        //
        /**  newThread() is hardly ever a good choice,*/
        // not only because of the latency involved when starting a thread, but also because this thread is not reused. Stack space must be allocated up front (typically around one megabyte, as controlled by the -Xss parameter of the JVM) and the operating system must start new native thread.
        //be useful only when tasks are coarse-grained: it takes a lot of time to complete but there are very few of them
        Schedulers.newThread();

        /**Schedulers.io() is almost always a better choice.*/
        //Consider using this scheduler for I/O bound tasks which require very little CPU resources. However they tend to take quite some time, waiting for network or disk. Thus, it is a good idea to have a relatively big pool of threads
        Schedulers.io();
        //you should use a computation scheduler when tasks are entirely CPU-bound
        //by default limits the number of threads running in parallel to the value of availableProcessors(), as found in the Runtime.getRuntime() utility class.
        //rx.scheduler.max-computation-threads system property.
        //uses unbounded queue in front of every thread
        Schedulers.computation();

        //Schedulers.from

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MyPool-%d").build();
        Executor executor = new ThreadPoolExecutor(10,  //corePoolSize
            10,  //maximumPoolSize
            0L, TimeUnit.MILLISECONDS, //keepAliveTime, unit
            new LinkedBlockingQueue<>(1000),  //workQueue
            threadFactory);

        /*
        Creating schedulers from Executor that we consciously configured is advised for projects dealing with high load.
        However, because RxJava has no control over independently created threads in an Executor,
        it cannot pin threads (that is, try to keep work of the same task on the same thread to improve cache locality).
        This Scheduler barely makes sure a single Scheduler.Worker (see “Scheduler implementation details overview”) processes events sequentially.
         */
        Scheduler scheduler = Schedulers.from(executor);

        // However, as opposed to immediate(), the upcoming task is executed when all previously scheduled tasks complete. immediate() invokes a given task right away,
        Schedulers.immediate();//avoid this scheduler
        Schedulers.trampoline();

        /*

        1044    | main  | Main start
        1094    | main  |  Outer start
        2097    | main  |   Inner start
        3097    | main  |   Inner end
        3100    | main  |  Outer end
        3100    | main  | Main end

        if use Schedulers.trampoline()
1030    | main  | Main start
1096    | main  |  Outer start
2101    | main  |  Outer end
2101    | main  |   Inner start
3101    | main  |   Inner end
3101    | main  | Main end
         */
        scheduler = Schedulers.immediate();
        Scheduler.Worker worker = scheduler.createWorker();
        System.out.println(("Main start"));
        worker.schedule(() -> {
            System.out.println(" Outer start");
            //sleepOneSecond();
            worker.schedule(() -> {
                System.out.println("  Inner start");
                //      sleepOneSecond();
                System.out.println("  Inner end");
            });
            System.out.println(" Outer end");
        });
        System.out.println("Main end");
        worker.unsubscribe();

        //  This Scheduler is used only for testing purposes
        Schedulers.test();


        /**Declarative Subscription with subscribeOn()*/

        ///subscribe() by default uses the client thread
        /*
        33  | main  | Starting
        120 | main  | Created
        128 | main  | Transformed
        133 | main  | Subscribed
        133 | main  | Got A
        133 | main  | Got B
        133 | main  | Completed
        134 | main  | Exiting


 runs in the main thread
*/

        System.out.println("Starting");
        final Observable<String> obs = simple();
        System.out.println("Created");
        final Observable<String> obs2 = obs.map(x -> x).filter(x -> true);
        System.out.println("Transformed");
        obs2.subscribe(x -> System.out.println("Got " + x), Throwable::printStackTrace,
            () -> System.out.println("Completed"));
        System.out.println("Exiting");

        //RxJava does not inject any concurrency facilities by default between Observable and Subscriber. The reason behind that is that Observables
        // tend to be backed by other concurrency mechanisms like event loops or custom threads

        /*

        35  | main  | Starting
112 | main  | Created
123 | main  | Exiting
123 | Sched-A-0 | Subscribed
124 | Sched-A-0 | Got A
124 | Sched-A-0 | Got B
124 | Sched-A-0 | Completed

         */

        /**subscribeOn() is very seldom used
         *
         * Normally, Observables come from sources that are naturally asynchronous (like RxNetty
         *
         * You should treat subscribeOn() only in special cases when the underlying Observable is known to be synchronous (create() being blocking)
         * */
        ExecutorService poolA = newFixedThreadPool(10, threadFactory("Sched-A-%d"));
        Scheduler schedulerA = Schedulers.from(poolA);
        ExecutorService poolB = newFixedThreadPool(10, threadFactory("Sched-B-%d"));
        Scheduler schedulerB = Schedulers.from(poolB);

        ExecutorService poolC = newFixedThreadPool(10, threadFactory("Sched-C-%d"));
        Scheduler schedulerC = Schedulers.from(poolC);


        System.out.println("Starting");
        final Observable<String> obs1 = simple();
        System.out.println("Created");
        obs1.subscribeOn(schedulerA)
            .subscribe(x -> System.out.println("Got " + x), Throwable::printStackTrace,
                () -> System.out.println("Completed"));
        System.out.println("Exiting");



/*

17  | main  | Starting
73  | main  | Created
83  | main  | Exiting
84  | Sched-A-0 | Subscribed
84  | Sched-A-0 | Got A
84  | Sched-A-0 | Got B
84  | Sched-A-0 | Completed
 */


        System.out.println("Starting");
        Observable<String> obs3 = simple();
        System.out.println("Created");
        //closest to the original Observable wins
        obs3.subscribeOn(schedulerA)
            //many other operators
            //multiple subscribeOn() are not only ignored, but also introduce small overhead.
            .subscribeOn(schedulerB)
            .subscribe(x -> System.out.println("Got " + x), Throwable::printStackTrace,
                () -> System.out.println("Completed"));
        System.out.println("Exiting");


        //all operators are executed by default in the same thread (scheduler), no concurrency is involved by default:
/*
20  | main  | Starting
104 | main  | Created
123 | main  | Exiting
124 | Sched-A-0 | Subscribed
124 | Sched-A-0 | A
124 | Sched-A-0 | A1
124 | Sched-A-0 | A12
124 | Sched-A-0 | Got A12
124 | Sched-A-0 | B
124 | Sched-A-0 | B1
124 | Sched-A-0 | B12
125 | Sched-A-0 | Got B12


 */

        //RxJava creates a single Worker instance (see: “Scheduler implementation details overview”) for the entire pipeline, mostly to guarantee sequential processing of events.
        System.out.println("Starting");
        Observable<String> obs4 = simple();
        System.out.println("Created");
        obs4.doOnNext(System.out::println).map(x -> x + '1').doOnNext(System.out::println)
            .map(x -> x + '2')
            //Remember that the position of subscribeOn()  is not relevant
            .subscribeOn(schedulerA).doOnNext(System.out::println)
            .subscribe(x -> System.out.println("Got " + x), Throwable::printStackTrace,
                () -> System.out.println("Completed"));
        System.out.println("Exiting");


        /**flatMap() comes to the rescue. Rather than blocking within map(), we can invoke flatMap() and asynchronously collect all the results
         * flatMap() and merge() are the operators when we want to achieve true parallelism
         *
         * Observable using flatMap() where each internal Observable has subscribeOn() works like ForkJoinPool from java.util.concurrent,
         * where each substream is a fork of execution and flatMap() is a safe join stage
         *
         *
         * */


        /*

        113  | Sched-A-1 | Purchasing 1 butter
114  | Sched-A-0 | Purchasing 1 bread
125  | Sched-A-2 | Purchasing 1 milk
125  | Sched-A-3 | Purchasing 1 tomato
126  | Sched-A-4 | Purchasing 1 cheese
1126 | Sched-A-2 | Done 1 milk
1126 | Sched-A-0 | Done 1 bread
1126 | Sched-A-1 | Done 1 butter
1128 | Sched-A-3 | Done 1 tomato
1128 | Sched-A-4 | Done 1 cheese

         */
        RxGroceries rxGroceries = new RxGroceries();
        Observable<BigDecimal> totalPrice =
            Observable.just("bread", "butter", "milk", "tomato", "cheese")
                .flatMap(prod -> rxGroceries.purchase(prod, 1).subscribeOn(schedulerA))
                .reduce(BigDecimal::add).single();



        /**
         * Of course, the preceding tips only apply to blocking Observables, which are rarely seen in real applications.
         * If your underlying Observables are already asynchronous, achieving concurrency is a matter of understanding how they are combined and when subscription occurs.
         * For example, merge() on two streams will subscribe to both of them concurrently,
         * whereas the concat() operator waits until the first stream finishes before it subscribes to the second one
         *
         * */

        /**Batching Requests Using groupBy()*/


        /*
        This code is quite complex, so before revealing the output, let’s quickly go through it.
        First, we group products simply by their name, thus identity function prod -> prod.
        In return we get an awkward Observable<GroupedObservable<String, String>>.
        There is nothing wrong with that. Next, flatMap() receives each GroupedObservable<String, String>,
        representing all products of the same name. So, for example, there will be an ["egg", "egg", "egg"]
        Observable there with a key "egg", as well. If groupBy() used a different key function, like prod.length(), the same sequence would have a key 3.

164  | Sched-A-0 | Purchasing 1 bread
165  | Sched-A-1 | Purchasing 1 butter
166  | Sched-A-2 | Purchasing 3 egg
166  | Sched-A-3 | Purchasing 1 milk
166  | Sched-A-4 | Purchasing 2 tomato
166  | Sched-A-5 | Purchasing 1 cheese
1151 | Sched-A-0 | Done 1 bread
1178 | Sched-A-1 | Done 1 butter
1180 | Sched-A-5 | Done 1 cheese
1183 | Sched-A-3 | Done 1 milk
1253 | Sched-A-4 | Done 2 tomato
1354 | Sched-A-2 | Done 3 egg

         */
        Observable<BigDecimal> totalPrice1 = Observable
            .just("bread", "butter", "egg", "milk", "tomato", "cheese", "tomato", "egg", "egg")
            .groupBy(prod -> prod).flatMap(grouped -> grouped.count().map(quantity -> {
                String productName = grouped.getKey();
                return Pair.of(productName, quantity);
            })).flatMap(order -> rxGroceries.purchase(order.getKey(), order.getValue())
                .subscribeOn(schedulerA)).reduce(BigDecimal::add).single();

        /**Declarative Concurrency with observeOn()
         *
         * Conversely, observeOn() controls which Scheduler is used to invoke downstream Subscribers occurring after observeOn()
         * */


        /*
observeOn() occurs somewhere in the pipeline chain, and this time, as opposed to subscribeOn(), the position of observeOn() is quite important. No matter what Scheduler was running operators above observeOn() (if any), everything below uses the supplied Scheduler. In this example, there is no subscribeOn(), so the default is applied (no concurrency):

23  | main  | Starting
136 | main  | Created
163 | main  | Subscribed
163 | main  | Found 1: A
163 | main  | Found 1: B
163 | main  | Exiting
163 | Sched-A-0 | Found 2: A
164 | Sched-A-0 | Got 1: A
164 | Sched-A-0 | Found 2: B
164 | Sched-A-0 | Got 1: B
164 | Sched-A-0 | Completed


         */

        System.out.println("Starting");
        final Observable<String> obs5 = simple();
        System.out.println("Created");
        obs5.doOnNext(x -> System.out.println("Found 1: " + x)).observeOn(schedulerA)
            .doOnNext(x -> System.out.println("Found 2: " + x))
            .subscribe(x -> System.out.println("Got 1: " + x), Throwable::printStackTrace,
                () -> System.out.println("Completed"));
        System.out.println("Exiting");


        /*
        All of the operators above observeOn are executed within client thread, which happens to be the default in RxJava. But below observeOn(), the operators are executed within the supplied Scheduler. This will become even more obvious when both subscribeOn() and multiple observeOn() occur within the pipeline:
21  | main  | Starting
98  | main  | Created
108 | main  | Exiting
129 | Sched-A-0 | Subscribed
129 | Sched-A-0 | Found 1: A
129 | Sched-A-0 | Found 1: B
130 | Sched-B-0 | Found 2: A
130 | Sched-B-0 | Found 2: B
130 | Sched-C-0 | Found 3: A
130 | Sched-C-0 | Got: A
130 | Sched-C-0 | Found 3: B
130 | Sched-C-0 | Got: B
130 | Sched-C-0 | Completed
Remember, everything below observeOn() is run within the supplied Scheduler, of course until another observeOn() is encountered. Additionally subscribeOn() can occur anywhere between Observable and subscribe(), but this time it only affects operators down to the first observeOn():

         */

        /**typically there is just one subscribeOn() and observeOn() in the pipeline of operators
         *
         * subscribeOn() can be placed close to the original Observable to improve readability, whereas observeOn() is close to subscribe() so that only Subscriber uses that special Scheduler, other operators rely on the Scheduler from subscribeOn().
         *
         * */

        System.out.println("Starting");
        final Observable<String> obs6 = simple();
        System.out.println("Created");
        obs6.doOnNext(x -> System.out.println("Found 1: " + x)).observeOn(schedulerB)
            .doOnNext(x -> System.out.println("Found 2: " + x)).observeOn(schedulerC)
            .doOnNext(x -> System.out.println("Found 3: " + x)).subscribeOn(schedulerA)
            .subscribe(x -> System.out.println("Got 1: " + x), Throwable::printStackTrace,
                () -> System.out.println("Completed"));
        System.out.println("Exiting");


        //Here is a more advanced program that takes advantage of these two operators:
        /*
        26   | main  | Starting
93   | main  | Created
121  | main  | Exiting

122  | Sched-A-0 | Subscribed
124  | Sched-B-0 | Storing A
124  | Sched-B-1 | Storing B
124  | Sched-B-2 | Storing C
124  | Sched-B-3 | Storing D

1136 | Sched-C-1 | Got: 44b8b999-e687-485f-b17a-a11f6a4bb9ce
1136 | Sched-C-1 | Got: 532ed720-eb35-4764-844e-690327ac4fe8
1136 | Sched-C-1 | Got: 13ddf253-c720-48fa-b248-4737579a2c2a
1136 | Sched-C-1 | Got: 0eced01d-3fa7-45ec-96fb-572ff1e33587
1137 | Sched-C-1 | Completed

         */
        log("Starting");
        Observable<String> obs7 = Observable.create(subscriber -> {
            log("Subscribed");
            subscriber.onNext("A");
            subscriber.onNext("B");
            subscriber.onNext("C");
            subscriber.onNext("D");
            subscriber.onCompleted();
        });
        log("Created");
        obs7.subscribeOn(schedulerA).flatMap(record -> store(record).subscribeOn(schedulerB))
            .observeOn(schedulerC)
            .subscribe(x -> log("Got: " + x), Throwable::printStackTrace, () -> log("Completed"));
        log("Exiting");

        /**This leads to an interesting conclusion: RxJava controls concurrency with just two operators (subscribeOn() and observeOn()), but the more you use reactive extensions, the less frequently you will see these in production code.*/


        /**Schedulers.computation()*/
        /*
        Without supplying a custom schedulerA, all operators below delay() would use the computation() Scheduler
       it would consume one Worker from globally shared computation() scheduler

         interval(), range(), timer(), repeat(), skip(), take(), timeout(), and several others that have yet to be introduced.
         If you do not provide a scheduler to such operators, computation() Scheduler is utilized, which is a safe default in most cases.


         */
        Observable.just("A", "B").delay(1, SECONDS, schedulerA).subscribe(this::log);

    }



    private ThreadFactory threadFactory(String pattern) {
        return new ThreadFactoryBuilder().setNameFormat(pattern).build();
    }

    void log(String msg) {
        System.out.println(msg);


    }

    Observable<UUID> store(String s) {
        return Observable.create(subscriber -> {
            log("Storing " + s);
            //hard work
            subscriber.onNext(UUID.randomUUID());
            subscriber.onCompleted();
        });
    }


    class RxGroceries {

        Observable<BigDecimal> purchase(String productName, int quantity) {
            return Observable.fromCallable(() -> doPurchase(productName, quantity));
        }

        BigDecimal doPurchase(String productName, int quantity) {
            System.out.println("Purchasing " + quantity + " " + productName);
            //real logic here
            System.out.println("Done " + quantity + " " + productName);
            return new BigDecimal(111.11);
        }

    }
}