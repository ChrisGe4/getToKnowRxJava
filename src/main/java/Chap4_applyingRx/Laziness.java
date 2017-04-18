package Chap4_applyingRx;

import rx.Observable;

import java.util.List;

/**
 * @author Chris.Ge
 */
public class Laziness {

    private static final int PAGE_SIZE = 10;
    static Dao personDao = new Dao();

    public static void main(String[] args) {
        /**to blocking*/
        List<Person> people = personDao.listPeople()
            // This lazy operator will buffer all Person events and keep them in memory until the onCompleted() event is received
            .toList()//
            .toBlocking()//
            //A similar operator, first(), will return a value of T and discard whatever it has left. single(), on the other hand,
            // makes sure there are no more pending events in underlying Observable before terminating.
            // This means single() will block waiting for onCompleted() callback. Here is the same code snippet as earlier, this time with all operators chained:
            .single();
        /**Embracing Laziness
         *
         * see bestBookFor
         * */


        /**Composing Observables - eg pagination*/

        //before
        // -->    List<Person> listPeople(int page)


        //after
        //-->Observable<Person> allPeople(int initialPage)



        /**Lazy paging and concatenation
         *
         * No matter which approach you choose, all transformations on Person object are lazy. As long as you limit the number of records you want to process
         * (for example with people.take(15)), the Observable<Person> will invoke listPeople() as late as possible.
         *
         * */

        //If this were not RxJava, the preceding code would take an enormous amount of time and memory, basically loading the entire database to memory.
        // But because Observable is lazy, no query to the database appeared yet. Moreover, if we find an empty page it means all further pages are empty,
        // as well (we reached the end of the table). Therefore, we use takeWhile() rather than filter()
        Observable<List<Person>> allPages = Observable//
            .range(0, Integer.MAX_VALUE)//
            .map(i -> listPeople(i))//
            .takeWhile(list -> !list.isEmpty());

        //To flatten allPages to Observable<Person> we can use concatMap()
        //concatMap() requires a transformation from List<Person> to Observable<Person>, executed for each page
        // from can take iterable

        Observable<Person> people1 = allPages.concatMap(Observable::from);
        //equals to
        Observable<Person> people2 = allPages.concatMapIterable(page -> page);
    }

    static Observable<Person> allPeople(int initialPage) {
        //when the Observable on the left is completed, rather than propagating completion notification to subscribers, subscribe to Observable on the right and continue as if nothing happened,
        // as depicted in the following marble diagram:
        /**
         * dont quite get this part
         *
         * allPeople(initialPage) calls allPeople(initialPage + 1) without any stop condition. This is a recipe for StackOverflowError in most languages,
         * but not here. Again, calling allPeople() is always lazy, therefore the moment you stop listening (unsubscribe), this recursion is over.
         * Technically concatWith() can still produce StackOverflowError here.
         *
         */

        return Observable.defer(() -> Observable.from(listPeople(initialPage)))
            .concatWith(Observable.defer(() -> Observable.from(listPeople(initialPage + 1))));

    }

    static List<Person> listPeople(int page) {
        return query("SELECT * FROM PEOPLE ORDER BY id LIMIT ? OFFSET ?", PAGE_SIZE,
            page * PAGE_SIZE);
    }

    static List<Person> query(String s, int pageSize, int offSet) {
        return null;
    }

    private void bestBookFor(Person person) {

        //before
        /*

         Book book;
    try {
        book = recommend(person);
    } catch (Exception e) {
        book = bestSeller();
    }
    display(book.getTitle());

         */

        //after

        //        Observable<Book> recommended = recommend(person);
        //        Observable<Book> bestSeller = bestSeller();
        //        Observable<Book> book = recommended.onErrorResumeNext(bestSeller);
        //        Observable<String> title = book.map(Book::getTitle);
        //        title.subscribe(this::display);

        /*
        onErrorResumeNext() is a powerful operator that intercepts exceptions happening upstream, swallows them,
        and subscribes to provided backup Observable. This is how Rx implements a try-catch clause. We will spend much more time on error
        handling later in this book (see “Declarative try-catch Replacement”). For the time being, notice how we can lazily call bestSeller() without
        worrying that fetching best seller happens even when a real recommendation went fine.


         */
        recommend(person).onErrorResumeNext(bestSeller()).map(Book::getTitle)
            .subscribe(this::display);

    }

    private Observable<Book> recommend(Person person) {
        return null;

    }

    private Observable<Book> bestSeller() {
        return null;
    }

    private void display(String s) {
    }


    private static class Person {
    }


    private static class Dao {
        public Observable<Person> listPeople() {
            final List<Person> people = query("SELECT * FROM PEOPLE");
            return Observable.from(people);
        }

        /**
         * Embracing Laziness
         */
        public Observable<Person> listPeopleLazy() {
            //Because Observable is lazy, calling listPeople() has no side effects and almost no performance footprint. No database is queried yet
            return Observable.defer(() -> Observable.from(query("SELECT * FROM PEOPLE")));
        }


        private List<Person> query(String s) {
            return null;
        }
    }


    private static class Book {
        public static String getTitle(Book book) {
            return null;
        }
    }
}
