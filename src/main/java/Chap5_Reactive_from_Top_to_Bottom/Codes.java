package Chap5_Reactive_from_Top_to_Bottom;

/**
 * Created by chrisge on 9/11/17.
 */
public class Codes {

  public static void main(String[] args) {
    Observable<TravelAgency> agencies = agencies();
    Observable<User> user = rxFindById(id);
    Observable<GeoLocation> location = rxLocate();
//zipWith() takes two independent Observable s and asynchronously waits for both
    Observable<Ticket> ticket = user.zipWith(location,
        (us, loc) -> agencies.flatMap(agency -> agency.rxSearch(us, loc)).first()).flatMap(x -> x)
        .flatMap(this::rxBook);
    //or

    ticket = user
        .zipWith(location, (usr, loc) -> Pair.of(usr, loc))
        .flatMap(pair -> agencies
            .flatMap(agency -> {
              User usr = pair.getLeft();
              GeoLocation loc = pair.getRight();
              return agency.rxSearch(usr, loc);
            }))
        .first()
        .flatMap(this::rxBook);
  }
}
