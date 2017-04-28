package Chap4_applyingRx;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.*;

/**
 * @author Chris.Ge
 */
public class Schedulers_example {

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
        System.out.println(("Main start"); worker.schedule(() -> {
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


    }

}
