package gingrasf.campsiteManager;

import gingrasf.campsiteManager.persistence.AvailableDateLockRepository;
import gingrasf.campsiteManager.persistence.CampsiteRepository;
import gingrasf.campsiteManager.model.CampsiteAvailability;
import gingrasf.campsiteManager.model.CampsiteReservation;
import gingrasf.campsiteManager.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static gingrasf.campsiteManager.TestUtil.buildValidUser;
import static gingrasf.campsiteManager.TestUtil.generateMultiDayReservation;
import static gingrasf.campsiteManager.TestUtil.generateOneDayReservationsBetween;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class CampsiteServiceTest {

    public static final int MAX_RESERVATION_DURATION = 3;

    @Mock
    CampsiteRepository repository;

    @Mock
    AvailableDateLockRepository availableDateLockRepository;

    @Mock
    CampsiteReservationValidator validator;

    CampsiteService service;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        service = new CampsiteService(repository, availableDateLockRepository, validator);
        when(availableDateLockRepository.lockAvailableDate(any(), any())).thenReturn(true);
    }

    @Test(expected = RuntimeException.class)
    public void whenRepositoryThrowsAnExceptionPropagateIt() {
        final LocalDate now = LocalDate.now();
        final LocalDate until = now.plusMonths(2);

        doThrow(new RuntimeException("BOOM!")).when(repository.findAll());

        service.getAvailabilityBetween(now, until);
    }


    // Get Availability Cases
    @Test
    public void whenQueryingAvailabilityWeGetBasicInfo() {
        final LocalDate now = LocalDate.now();
        final LocalDate until = now.plusMonths(2);

        when(repository.findAll()).thenReturn(Collections.emptyList());

        final CampsiteAvailability campsiteAvailability = service.getAvailabilityBetween(now, until);

        assertThat(campsiteAvailability).isNotNull();
        assertThat(campsiteAvailability.getSearchPeriodStart()).isEqualTo(now);
        assertThat(campsiteAvailability.getSearchPeriodEnd()).isEqualTo(until);
        assertThat(campsiteAvailability.getAvailableDates()).isNotEmpty();
    }

    @Test
    public void whenNoReservationReturnAllTheDates() {
        final LocalDate now = LocalDate.now();
        final LocalDate until = now.plusMonths(2);
        final long expectedNbOfDays = DAYS.between(now, until);

        when(repository.findAll()).thenReturn(Collections.emptyList());

        final CampsiteAvailability campsiteAvailability = service.getAvailabilityBetween(now, until);

        assertThat(campsiteAvailability.getAvailableDates().size()).isEqualTo(expectedNbOfDays);
    }

    @Test
    public void whenNoReservationTomorrowIsAvailable() {
        final LocalDate now = LocalDate.now();
        final LocalDate until = now.plusMonths(2);
        final LocalDate tomorrow = now.plusDays(1);

        when(repository.findAll()).thenReturn(Collections.emptyList());

        final CampsiteAvailability campsiteAvailability = service.getAvailabilityBetween(now, until);

        assertThat(campsiteAvailability.getAvailableDates()).contains(tomorrow);
    }

    @Test
    public void whenThereIsAReservationForTomorrowDoNotReturnThatDate() {
        final LocalDate now = LocalDate.now();
        final LocalDate until = now.plusMonths(2);
        final LocalDate tomorrow = now.plusDays(1);
        final long expectedNbOfDays = DAYS.between(now, until) - 1;

        when(repository.findAll()).thenReturn(generateOneDayReservationsBetween(tomorrow, tomorrow.plusDays(1)));

        final CampsiteAvailability campsiteAvailability = service.getAvailabilityBetween(now, until);

        assertThat(campsiteAvailability.getAvailableDates().size()).isEqualTo(expectedNbOfDays);
        assertThat(campsiteAvailability.getAvailableDates()).doesNotContain(tomorrow);
    }

    @Test
    public void whenAllTheDatesAreReservedReturnNoAvailability() {
        final LocalDate now = LocalDate.now();
        final LocalDate until = now.plusMonths(1);

        when(repository.findAll()).thenReturn(generateOneDayReservationsBetween(now, until));

        final CampsiteAvailability campsiteAvailability = service.getAvailabilityBetween(now, until);

        assertThat(campsiteAvailability.getAvailableDates()).isEmpty();
    }

    @Test
    public void whenThereIsAMultiDaysReservationThoseDatesAreNotAvailable() {
        final LocalDate now = LocalDate.now();
        final LocalDate until = now.plusMonths(2);
        final LocalDate tomorrow = now.plusDays(1);
        final LocalDate dayAfterTomorrow = tomorrow.plusDays(1);
        final long expectedNbOfDays = DAYS.between(now, until) - 3;

        when(repository.findAll()).thenReturn(generateMultiDayReservation(now, 3));

        final CampsiteAvailability campsiteAvailability = service.getAvailabilityBetween(now, until);

        assertThat(campsiteAvailability.getAvailableDates().size()).isEqualTo(expectedNbOfDays);
        assertThat(campsiteAvailability.getAvailableDates()).doesNotContain(now);
        assertThat(campsiteAvailability.getAvailableDates()).doesNotContain(tomorrow);
        assertThat(campsiteAvailability.getAvailableDates()).doesNotContain(dayAfterTomorrow);

    }

    @Test
    public void whenQueryingAvailabilityUntilTomorrowAndTodayIsFreeItShouldBeReturned() {
        final LocalDate now = LocalDate.now();
        final LocalDate tomorrow = now.plusDays(1);

        when(repository.findAll()).thenReturn(Collections.emptyList());

        final CampsiteAvailability campsiteAvailability = service.getAvailabilityBetween(now, tomorrow);

        assertThat(campsiteAvailability.getAvailableDates()).contains(now);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenQueryingAvailabilityUntilTodayThrowException() {
        final LocalDate now = LocalDate.now();
        service.getAvailabilityBetween(now, now);
    }


    @Test(expected = IllegalArgumentException.class)
    public void whenQueryingAvailabilityForAPastDateThrowException() {
        final LocalDate now = LocalDate.now();
        final LocalDate yesterday = now.minusDays(1);
        service.getAvailabilityBetween(yesterday, now);
    }

    // Get Availability Cases
    @Test
    public void whenRequestingAllReservationsWeGetThem() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate until = start.plusDays(1);
        final List<CampsiteReservation> existingCampsiteReservations = generateOneDayReservationsBetween(start, until);

        when(repository.findAll()).thenReturn(existingCampsiteReservations);
        final Iterable<CampsiteReservation> campsiteReservations = service.getAllReservation();

        assertThat(campsiteReservations).isNotNull();
        assertThat(campsiteReservations).isNotEmpty();
        assertThat(campsiteReservations).containsExactly(existingCampsiteReservations.toArray(new CampsiteReservation[existingCampsiteReservations.size()]));
    }

    @Test
    public void whenRequestingAllReservationsAndThereAreNoneWeGetEmptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        final Iterable<CampsiteReservation> campsiteReservations = service.getAllReservation();

        assertThat(campsiteReservations).isNotNull();
        assertThat(campsiteReservations).isEmpty();
    }

    // Create cases
    @Test
    public void whenCreatingAOneDayReservationForTomorrowItShouldWork() throws InterruptedException {
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        final LocalDate dayAfterTomorrow = tomorrow.plusDays(1);
        final User validUser = buildValidUser();

        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        final CampsiteReservation campsiteReservation = service.createReservation(validUser, tomorrow, dayAfterTomorrow);

        assertThat(campsiteReservation).isNotNull();
        assertThat(campsiteReservation.getStartDate()).isEqualTo(tomorrow);
        assertThat(campsiteReservation.getEndDate()).isEqualTo(dayAfterTomorrow);
        assertThat(campsiteReservation.getUser()).isEqualTo(validUser);
        assertThat(campsiteReservation.getId()).isNotEmpty();
    }

    @Test
    public void whenCreatingAReservationForMaxDurationForTomorrowItShouldWork() throws InterruptedException {
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        final LocalDate until = tomorrow.plusDays(MAX_RESERVATION_DURATION);
        final User validUser = buildValidUser();

        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        final CampsiteReservation campsiteReservation = service.createReservation(validUser, tomorrow, until);

        assertThat(campsiteReservation).isNotNull();
        assertThat(campsiteReservation.getStartDate()).isEqualTo(tomorrow);
        assertThat(campsiteReservation.getEndDate()).isEqualTo(until);
        assertThat(campsiteReservation.getUser()).isEqualTo(validUser);
        assertThat(campsiteReservation.getId()).isNotEmpty();
    }

    @Test
    public void whenCreatingAReservationInOneMonthItShouldWork() throws InterruptedException {
        final LocalDate inOneMonth = LocalDate.now().plusMonths(1);
        final LocalDate start = inOneMonth;
        final LocalDate until = inOneMonth.plusDays(1);
        final User validUser = buildValidUser();

        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        final CampsiteReservation campsiteReservation = service.createReservation(validUser, start, until);

        assertThat(campsiteReservation).isNotNull();
        assertThat(campsiteReservation.getStartDate()).isEqualTo(start);
        assertThat(campsiteReservation.getEndDate()).isEqualTo(until);
        assertThat(campsiteReservation.getUser()).isEqualTo(validUser);
        assertThat(campsiteReservation.getId()).isNotEmpty();
    }

    @Test(expected = CampsiteReservationConflictException.class)
    public void whenCreatingAReservationForADayAlreadyReservedThrowConflictException() throws InterruptedException {
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        final LocalDate until = tomorrow.plusDays(1);
        final User validUser = buildValidUser();

        when(repository.findAll()).thenReturn(generateOneDayReservationsBetween(tomorrow, tomorrow.plusDays(1)));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        service.createReservation(validUser, tomorrow, until);
    }

    @Test(expected = CampsiteReservationConflictException.class)
    public void whenCreatingAReservationForMultiDaysWithOneAlreadyReservedThrowConflictException() throws InterruptedException {
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        final LocalDate until = tomorrow.plusDays(MAX_RESERVATION_DURATION);
        final User validUser = buildValidUser();

        when(repository.findAll()).thenReturn(generateOneDayReservationsBetween(tomorrow, tomorrow.plusDays(1)));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        service.createReservation(validUser, tomorrow, until);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenCreatingAnInvalidReservationThrowException() throws InterruptedException {
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        final LocalDate until = tomorrow.plusDays(MAX_RESERVATION_DURATION + 1);
        final User validUser = buildValidUser();

        doThrow(new IllegalArgumentException("Invalid!")).when(validator).validateReservation(any(LocalDate.class), any(LocalDate.class));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        service.createReservation(validUser, tomorrow, until);
    }


    // Update cases
    @Test
    public void whenShiftingAnExistingReservationToAnotherAvailableTimeItShouldWork() throws InterruptedException {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate end = start.plusDays(1);
        final User validUser = buildValidUser();
        final String existingId = "some-test-unique-id";
        final CampsiteReservation existingReservation = CampsiteReservation.builder().id(existingId).startDate(start).endDate(end).user(validUser).build();

        final LocalDate newStart = start.plusDays(3);
        final LocalDate newEnd = end.plusDays(3);
        final CampsiteReservation newReservation = CampsiteReservation.builder().id(existingId).startDate(newStart).endDate(newEnd).user(validUser).build();

        when(repository.findById(existingId)).thenReturn(ofNullable(existingReservation));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);



        final CampsiteReservation campsiteReservation = service.updateReservation(existingId, newReservation);

        assertThat(campsiteReservation.getStartDate()).isEqualTo(newStart);
        assertThat(campsiteReservation.getEndDate()).isEqualTo(newEnd);
        assertThat(campsiteReservation.getUser()).isEqualTo(validUser);
        assertThat(campsiteReservation.getId()).isEqualTo(existingId);
    }

    @Test(expected = CampsiteReservationConflictException.class)
    public void whenShiftingAnExistingReservationToANonAvailableTimeThrowConflictException() throws InterruptedException {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate end = start.plusDays(1);
        final User validUser = buildValidUser();
        final String existingId = "some-test-unique-id";
        final CampsiteReservation existingReservation = CampsiteReservation.builder().id(existingId).startDate(start).endDate(end).user(validUser).build();

        final LocalDate newStart = start;
        final LocalDate newEnd = newStart.plusDays(MAX_RESERVATION_DURATION);
        final CampsiteReservation newReservation = CampsiteReservation.builder().id(existingId).startDate(newStart).endDate(newEnd).user(validUser).build();


        when(repository.findById(existingId)).thenReturn(ofNullable(existingReservation));
        when(repository.findAll()).thenReturn(generateOneDayReservationsBetween(newStart.plusDays(1), newStart.plusDays(2)));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        service.updateReservation(existingId, newReservation);
   }

    @Test(expected = NoSuchElementException.class)
    public void whenUpdatingAReservationThatDoesNotExistThrowNoSuchElementException() throws InterruptedException {
        final LocalDate start = LocalDate.now().plusDays(1);
        final User validUser = buildValidUser();
        final String nonExistingId = "some-test-unique-id";

        final LocalDate newStart = start;
        final LocalDate newEnd = newStart.plusDays(MAX_RESERVATION_DURATION);
        final CampsiteReservation newReservation = CampsiteReservation.builder().id(nonExistingId).startDate(newStart).endDate(newEnd).user(validUser).build();

        when(repository.findById(nonExistingId)).thenReturn(empty());
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        service.updateReservation(nonExistingId, newReservation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenUpdatingAReservationWithADifferentUserThrowException() throws InterruptedException {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate end = start.plusDays(1);
        final User validUser = buildValidUser();
        final User newGuy =  User.builder().email("newguy@test.com").fullName("New Guy").build();
        final String existingId = "some-test-unique-id";
        final CampsiteReservation existingReservation = CampsiteReservation.builder().id(existingId).startDate(start).endDate(end).user(validUser).build();

        final LocalDate newStart = start;
        final LocalDate newEnd = newStart.plusDays(MAX_RESERVATION_DURATION);
        final CampsiteReservation newReservation = CampsiteReservation.builder().id(existingId).startDate(newStart).endDate(newEnd).user(newGuy).build();

        doThrow(new IllegalArgumentException("Invalid!")).when(validator).validateReservation(any(LocalDate.class), any(LocalDate.class));
        when(repository.findById(existingId)).thenReturn(ofNullable(existingReservation));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        service.updateReservation(existingId, newReservation);
    }


    @Test(expected = IllegalArgumentException.class)
    public void whenUpdatingAReservationWithInvalidDatesThrowException() throws InterruptedException {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate end = start.plusDays(1);
        final User validUser = buildValidUser();
        final String existingId = "some-test-unique-id";
        final CampsiteReservation existingReservation = CampsiteReservation.builder().id(existingId).startDate(start).endDate(end).user(validUser).build();


        final LocalDate newStart = start;
        final LocalDate newEnd = newStart.plusDays(MAX_RESERVATION_DURATION);
        final CampsiteReservation newReservation = CampsiteReservation.builder().id(existingId).startDate(newStart).endDate(newEnd).user(validUser).build();


        doThrow(new IllegalArgumentException("Invalid!")).when(validator).validateReservation(any(LocalDate.class), any(LocalDate.class));
        when(repository.findById(existingId)).thenReturn(ofNullable(existingReservation));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        service.updateReservation(existingId, newReservation);
    }

    // Delete cases
    @Test
    public void whenDeletingAnExistingReservationItShouldWork() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate end = start.plusDays(1);
        final User validUser = buildValidUser();
        final String existingId = "some-test-unique-id";
        final CampsiteReservation existingReservation = CampsiteReservation.builder().id(existingId).startDate(start).endDate(end).user(validUser).build();

        when(repository.findById(existingId)).thenReturn(ofNullable(existingReservation));

        service.delete(existingId);
    }

    @Test(expected = NoSuchElementException.class)
    public void whenDeletingAReservationThatDoesNotExistThrowNoSuchElementException() {
        final String nonExistingId = "some-test-unique-id";

        when(repository.findById(nonExistingId)).thenReturn(empty());

        service.delete(nonExistingId);
    }
}
