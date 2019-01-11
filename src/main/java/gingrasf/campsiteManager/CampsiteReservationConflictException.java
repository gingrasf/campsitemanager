package gingrasf.campsiteManager;

import java.time.LocalDate;
import java.util.List;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;


/**
 * Raised when we try to reserved a non-available date for the Campsite.
 */
public class CampsiteReservationConflictException extends RuntimeException {

    public CampsiteReservationConflictException(List<LocalDate> reservedDates) {
        super(format("The campsite is not available for those dates: %s", join(",", dateListToString(reservedDates))));
    }

    private static List<String> dateListToString(List<LocalDate> reservedDates) {
        return reservedDates.stream().map(LocalDate::toString).collect(toList());
    }
}
