package gingrasf.campsiteManager;

import gingrasf.campsiteManager.model.CampsiteReservation;
import gingrasf.campsiteManager.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;

/**
 * Various util helper method for testing
 */
public final class TestUtil {

    public static List<CampsiteReservation> generateOneDayReservationsBetween(LocalDate start, LocalDate end) {
        final long nbOfDays = DAYS.between(start, end);
        return LongStream.range(0, nbOfDays).mapToObj(offset -> CampsiteReservation.builder().id("test-id-" + offset).user(buildValidUser()).startDate(start.plusDays(offset)).endDate(start.plusDays(offset + 1)).build()).collect(Collectors.toList());
    }

    public static List<CampsiteReservation> generateMultiDayReservation(LocalDate start, int nbOfDays) {
        return asList(CampsiteReservation.builder().id("test-id-multi").user(buildValidUser()).startDate(start).endDate(start.plusDays(nbOfDays)).build());
    }

    public static User buildValidUser() {
        return User.builder().email("test@test.com").fullName("Test Person").build();
    }

    public static CampsiteReservation buildReservationRequest(LocalDate start, int duration) {
        return buildReservationRequest(start, duration, buildValidUser());
    }

    public static CampsiteReservation buildReservationRequest(LocalDate start, int duration, User user) {
        return CampsiteReservation.builder().startDate(start).user(user).endDate(start.plusDays(duration)).build();
    }
}
