package Chap4_applyingRx;

import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mostof the time a single request is handled by a single thread. The samethread does the following:
 * Accepts TCP/IP connection->Parses HTTP request->Calls a controller or servlet->Blocks on database call->Processes results->Encodes response (e.g., in JSON)->Pushes raw bytes back to the client
 * <p>
 * <p>
 * It is not a bad practice to call subscribe() yourself, but try to push it out as far as possible.
 *
 * @author Chris.Ge
 */
public class Imperative_Concurrency {

    //    Imperative_Concurrency ic = new Imperative_Concurrency();



    public void run() {


        //blocking code

        Flight flight = lookupFlight("LOT 783");
        Passenger passenger = findPassenger(42);
        Ticket ticket = bookTicket(flight, passenger);
        sendEmail(ticket);

        //rx

        Observable<Flight> flight1 = rxLookupFlight("LOT 783");
        Observable<Passenger> passenger1 = rxFindPassenger(42);
        //But far more often, zip is simply used to join together two single-item Observables.
        Observable<Ticket> ticket1 = flight1.zipWith(passenger1, this::bookTicket);
        //It is not a bad practice to call subscribe() yourself, but try to push it out as far as possible.
        ticket1.subscribe(this::sendEmail);

        //schedulers

        // I used the Schedulers_example.io() factory method, but we can just as well use a custom ExecutorService and quickly wrap it with Scheduler
        /**but you should not see the usage of subscribeOn() (and yet to be described observeOn()) often. In real life, Observables come from asynchronous sources, so custom scheduling is not needed at all*/
        Observable<Flight> flight2 = rxLookupFlight("LOT 783").subscribeOn(Schedulers.io());
        Observable<Passenger> passenger2 = rxFindPassenger(42).subscribeOn(Schedulers.io());
        //As always, in case of errors, they are propagated downstream rather thanthrown arbitrarily.
        rxLookupFlight("LOT 783").subscribeOn(Schedulers.io()).timeout(100, TimeUnit.MILLISECONDS);

        //A good rule of thumb is that whenever you see double-wrapped type (for example Optional<Optional<...>>) there is a flatMap() invocation missing somewhere.
        Observable<Ticket> ticket2 = flight2.zipWith(passenger2, (f, p) -> Pair.of(f, p))
            .flatMap(pair -> rxBookTicket(pair.getLeft(), pair.getRight()));
        // or

        ticket2 = flight2.zipWith(passenger2, this::rxBookTicket).flatMap(obs -> obs);


        /**flatMap() as Asynchronous Chaining Operator*/
        /*
        In our sample application, we must now send a list of Tickets via e-mail. But we must keep in mind the following:

        The list can be potentially quite long.

        Sending an email might take several milliseconds or even seconds.

        The application must keep running gracefully in case of failures, but report in the end which tickets failed to be delivered.

         */
        //V1
        List<Ticket> tickets = new ArrayList<>();
        List<Ticket> failures = new ArrayList<>();
        for (Ticket t : tickets) {
            try {
                sendEmail(t);
            } catch (Exception e) {
                //log.warn("Failed to send {}", ticket, e)
                failures.add(t);
            }
        }
        //concurrent
        //use one loop,  .map(...).flatMap(...) will block
        ExecutorService pool = new ScheduledThreadPoolExecutor(1);

        List<Pair<Ticket, Future<SmtpResponse>>> tasks =
            //In real production code, you should consider a more meaningful and dedicated container like a TicketAsyncTask value object
            tickets.stream().map(t -> Pair.of(t, sendEmailAsync(t, pool)))
                .collect(Collectors.toList());
        failures = tasks.stream().flatMap(pair -> {
            try {
                Future<SmtpResponse> future = pair.getRight();
                future.get(10, TimeUnit.SECONDS);
                return Stream.empty();
            } catch (Exception e) {
                Ticket t = pair.getLeft();
                return Stream.of(t);
            }

        }).collect(Collectors.toList());

        //rx
        //ordinarily you would use subscribeOn() in the implementation so that the Observable is asynchronous by default.
        //As opposed to flatMap() (or merge) that subscribe immediately to all inner streams, concatMap (or concat) subscribes one inner Observable after another
        failures = Observable.from(tickets).flatMap(t -> rxSendEmail(t)//
            // inner flatMap() in our example ignores response and returns an empty stream.In such cases, flatMap() is an overkill
            .flatMap(r -> Observable.<Ticket>empty())//
            // .doOnError(e -> log.warn("Failed to send {}", ticket, e))//
            .onErrorReturn(err -> t)).toList().toBlocking().single();


        //final version


        Observable.from(tickets)//
            .flatMap(t ->//
                rxSendEmail(t)//
                    .ignoreElements()//
                    // .doOnError(e -> log.warn("Failed to send {}", ticket, e))//
                    // .onErrorReturn(err -> t)//
                    .subscribeOn(Schedulers.io()));


        //Polling Periodically for Changes -To track changes
        Observable.interval(10, TimeUnit.MILLISECONDS).map(x -> getOrderBookLength())
            .distinctUntilChanged();
        // Observable<Flight> observableNewFlights(){}

        /*
        Observable<Status> distinctUserIds = tweets        .distinct(status -> status.getUser().getId());
 extracted property (known as key) in distinct/distinctUntilChanged

 The important difference between distinct() and distinctUntilChanged() is that the latter can produce duplicates but only if they were separated by a different value
distinctUntilChanged() must only remember the last seen value, as opposed to distinct(), which must keep track of all unique values since the beginning of the stream.
         */
        Observable.interval(1, TimeUnit.SECONDS).flatMapIterable(x -> query()).distinct();


    }


    void log(Object label, long start) {
        System.out.println(
            System.currentTimeMillis() - start + "\t" + Thread.currentThread().getName() + "\t"
                + label);

    }

    List<Flight> query() {
        //take snapshot of file system directory
        //or database table
        return null;
    }

    String getOrderBookLength() {
        return null;

    }

    private Observable<SmtpResponse> rxSendEmail(Ticket ticket) {
        //unusual synchronous Observable
        return Observable.fromCallable(() -> sendEmail(ticket));
    }

    private Future<SmtpResponse> sendEmailAsync(Ticket ticket, ExecutorService pool) {
        return pool.submit(() -> sendEmail(ticket));
    }

    Flight lookupFlight(String flightNo) {
        //...
        return null;
    }

    Observable<Flight> rxLookupFlight(String flightNo) {
        return Observable.defer(() -> Observable.just(lookupFlight(flightNo)));
    }


    Passenger findPassenger(long id) {
        //...
        return null;

    }

    Observable<Passenger> rxFindPassenger(long id) {
        return Observable.defer(() -> Observable.just(findPassenger(id)));
    }

    Ticket bookTicket(Flight flight, Passenger passenger) {
        //...
        return null;

    }

    Observable<Ticket> rxBookTicket(Flight flight, Passenger passenger) {
        //...
        return null;
    }

    SmtpResponse sendEmail(Ticket ticket) {
        //...
        return null;

    }


    private class Flight {
    }


    private class Ticket {
    }


    private class Passenger {
    }


    private class SmtpResponse {
    }
}
