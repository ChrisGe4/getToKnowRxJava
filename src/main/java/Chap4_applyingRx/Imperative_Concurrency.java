package Chap4_applyingRx;

import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

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

        // I used the Schedulers.io() factory method, but we can just as well use a custom ExecutorService and quickly wrap it with Scheduler
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
