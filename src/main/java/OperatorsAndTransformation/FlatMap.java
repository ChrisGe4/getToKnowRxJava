package OperatorsAndTransformation;

import rx.Observable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.Observable.interval;

/**
 * @author Chris.Ge
 */
/*

The maxConcurrent parameter limits the number of ongoing inner Observables. In practice when flatMap() receives the first 10 Users it invokes loadProfile() for each of them.
However, when the 11th User appears from upstream,3 flatMap() will not even call loadProfile(). Instead, it will wait for any ongoing inner streams to complete.
Therefore, the maxConcurrent parameter limits the number of background tasks that are forked from flatMap().

 */

public class FlatMap {
    public static void main(String[] args) {
        //Now imagine that you would like to use a method returning an Iterable (like List or Set). For example, if Customer has a simple List<Order> getOrders(), you are forced to go through several operators to take advantage of it in Observable pipeline:

        Observable<Customer> customers = Observable.from(Arrays.asList());
        Observable<Order> orders =
            customers.flatMap(customer -> Observable.from(customer.getOrders()));
        //Or, equivalent and equally verbose:

        Observable<Order> orders1 = customers.map(Customer::getOrders).flatMap(Observable::from);
        // The need to map from a single item to Iterable is so popular that an operator, flatMapIterable(), was created to perform just such a transformation:

        Observable<Order> orders2 = customers.flatMapIterable(Customer::getOrders);


        /**You must take care when simply wrapping methods in an Observable. If getOrders() was not a simple getter but an expensive operation in terms of run time, it is better to implement getOrders() to explicitly return Observable<Order>.*/

        //flatMap() instead subscribes to all substreams immediately and merges them together, pushing events downstream whenever any of the inner streams emit anything.
        Observable.just(DayOfWeek.SUNDAY, DayOfWeek.MONDAY).

            flatMap(x -> loadRecordsFor(x));

        //keep observable order: Sun-0, Sun-1, Sun-2, Sun-3, Sun-4, Mon-0, Mon-1, Mon-2, Mon-3, Mon-4

        Observable.just(DayOfWeek.SUNDAY, DayOfWeek.MONDAY).concatMap(x -> loadRecordsFor(x))
            .forEach(System.out::println);

        /**concatMap(f) is semantically equivalent to flatMap(f, 1)—*/


        Observable.combineLatest(interval(17, MILLISECONDS).map(x -> "S" + x),
            interval(10, MILLISECONDS).map(x -> "F" + x), (s, f) -> f + ":" + s)
            .forEach(System.out::println);


        //merge:The merge() operator is used extensively when you want to treat multiple sources of events of the same type as a single source.
        //order of Observables passed to merge() is rather arbitrary.
        //obs1.mergeWith(obs2)
        //mergeDelayError() variant of merge() to postpone any errors until all of the other streams have finished. mergeDelayError() will even make sure to collect all exceptions, not only the first one, and encapsulate them in rx.exceptions.CompositeException .
        Observable<LicensePlate> all =
            Observable.merge(preciseAlgo("photo"), fastAlgo("photo"), experimentalAlgo("photo"));

        /**zip*/
        //create chessboard -no zip
        Observable<Integer> oneToEight = Observable.range(1, 8);
        Observable<String> ranks = oneToEight.map(Object::toString);
        Observable<String> files =
            oneToEight.map(x -> 'A' + x - 1).map(ascii -> (char) ascii.intValue())
                .map(ch -> Character.toString(ch));
        Observable<String> squares = files.flatMap(file -> ranks.map(rank -> file + rank));

        //plan a one-day vacation

        Observable<LocalDate> nextTenDays =
            Observable.range(1, 10).map(i -> LocalDate.now().plusDays(i));

        Observable<Vacation> possibleVacations =
            Observable.just(City.Warsaw, City.London, City.Paris)
                .flatMap(city -> nextTenDays.map(date -> new Vacation(city, date))).flatMap(
                vacation -> Observable.zip(vacation.weather().filter(Weather::isSunny),
                    vacation.cheapFlightFrom(City.NewYork), vacation.cheapHotel(),
                    (weather, flight, hotel) -> vacation));


    }



    //helper classes/methods
    static Observable<String> loadRecordsFor(DayOfWeek dow) {
        switch (dow) {
            case SUNDAY:
                return interval(90, MILLISECONDS).take(5).map(i -> "Sun-" + i);
            case MONDAY:
                return interval(65, MILLISECONDS).take(5).map(i -> "Mon-" + i);
            default:
                return null;
        }
    }



    static Observable<LicensePlate> fastAlgo(String photo) {
        //Fast but poor quality
        return null;
    }

    static Observable<LicensePlate> preciseAlgo(String photo) {
        //Precise but can be expensive
        return null;
    }

    static Observable<LicensePlate> experimentalAlgo(String photo) {
        //Unpredictable, running anyway
        return null;
    }

    private enum City {
        Warsaw,
        London,
        NewYork,
        Paris;
    }


    static class Vacation {
        private final City where;
        private final LocalDate when;

        Vacation(City where, LocalDate when) {
            this.where = where;
            this.when = when;
        }

        public Observable<Weather> weather() {
            //...
            return null;
        }

        public Observable<Flight> cheapFlightFrom(City from) {
            //...
            return null;
        }

        public Observable<Hotel> cheapHotel() {
            //...
            return null;
        }
    }


    private static class Weather {
        public static Boolean isSunny(Weather weather) {
            return true;
        }
    }


    private class LicensePlate {
    }


    private class CarPhoto {
    }


    private class Flight {
    }


    private class Hotel {
    }
}
