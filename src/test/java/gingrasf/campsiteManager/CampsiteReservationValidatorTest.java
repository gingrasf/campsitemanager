package gingrasf.campsiteManager;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static gingrasf.campsiteManager.CampsiteReservationValidator.MAX_RESERVATION_DURATION;
import static gingrasf.campsiteManager.TestUtil.buildValidUser;

public class CampsiteReservationValidatorTest {


    private CampsiteReservationValidator validator;

    @Before
    public void setup() {
        validator = new CampsiteReservationValidator();
    }

    @Test
    public void whenCreatingAValidReservationItShouldNotThrowAnyException() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate until = start.plusDays(2);
        validator.validateReservation(start, until);
    }

    @Test
    public void whenCreatingAValidReservationForMaxDurationItShouldNotThrowAnyException() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate until = start.plusDays(MAX_RESERVATION_DURATION);
        validator.validateReservation(start, until);
    }

    @Test
    public void whenCreatingAReservationWithValidUserItShouldNotThrowAnyException() {
        validator.validateUser(buildValidUser());
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenCreatingAReservationExceedingMaxDurationForTomorrowThrowException() {
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        final LocalDate until = tomorrow.plusDays(MAX_RESERVATION_DURATION + 1);
        validator.validateReservation(tomorrow, until);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenCreatingAReservationForTodayThrowException() {
        final LocalDate today = LocalDate.now();
        final LocalDate until = today.plusDays(1);
        validator.validateReservation(today, until);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenCreatingAReservationInMoreThanOneMonthThrowException() {
        final LocalDate inOneMonth = LocalDate.now().plusMonths(1);
        final LocalDate start = inOneMonth.plusDays(1);
        final LocalDate until = inOneMonth.plusDays(2);
        validator.validateReservation(start, until);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenCreatingAReservationInThePastThrowException() {
        final LocalDate start = LocalDate.now().minusDays(1);
        final LocalDate until = LocalDate.now();
        validator.validateReservation(start, until);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenCreatingAReservationThatStartAndEndTheSameDayThrowException() {
        final LocalDate start = LocalDate.now();
        final LocalDate until = start;
        validator.validateReservation(start, until);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenCreatingAReservationThatEndBeforeItStartThrowException() {
        final LocalDate start = LocalDate.now().plusDays(2);
        final LocalDate until = start.minusDays(1);
        validator.validateReservation(start, until);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenCreatingAReservationWithoutValidUserThrowException() {
        validator.validateUser(null);
    }
}
