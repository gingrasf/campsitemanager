package gingrasf.campsiteManager;

import gingrasf.campsiteManager.model.CampsiteAvailability;
import gingrasf.campsiteManager.model.CampsiteReservation;
import gingrasf.campsiteManager.model.User;
import gingrasf.campsiteManager.persistence.AvailableDateLockRepository;
import gingrasf.campsiteManager.persistence.CampsiteRepository;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toList;

public class CampsiteService {


    private final CampsiteRepository repository;
    private final AvailableDateLockRepository availableDateLockRepository;
    private final CampsiteReservationValidator validator;

    public CampsiteService(CampsiteRepository repository, AvailableDateLockRepository availableDateLockRepository, CampsiteReservationValidator validator) {
        this.repository = repository;
        this.validator = validator;
        this.availableDateLockRepository = availableDateLockRepository;
    }

    public CampsiteAvailability getAvailabilityBetween(LocalDate from, LocalDate until) {
        if (from.isBefore(LocalDate.now()) || until.isBefore(from)) {
            throw new IllegalArgumentException("Unable to query availability for a date in the past.");
        }
        if (until.isEqual(from)) {
            throw new IllegalArgumentException("The until parameter is exclusive, to see if today is available use tomorrow's date as the until parameter");
        }
        final List<LocalDate> reservedDates = getReservedDatesBetween(from, until);
        final List<LocalDate> availableDates = findLocalDateBetween(from, until).stream().filter(date -> !reservedDates.contains(date)).collect(toList());
        return CampsiteAvailability.builder().availableDates(availableDates).searchPeriodStart(from).searchPeriodEnd(until).build();
    }

    public CampsiteReservation createReservation(User user, LocalDate startDate, LocalDate endDate) throws InterruptedException {
        validator.validateReservation(startDate, endDate);
        validator.validateUser(user);

        if(lockDatesForProcessing(startDate, endDate)) {
            try {
                checkForAvailability(startDate, endDate);
                final CampsiteReservation reservation = CampsiteReservation.builder()
                        .id(UUID.randomUUID().toString())
                        .startDate(startDate)
                        .endDate(endDate)
                        .user(user)
                        .build();
                return repository.save(reservation);
            } finally {
                unlockDates(startDate, endDate);
            }
        }
        throw new CampsiteReservationConflictException(findLocalDateBetween(startDate, endDate));
    }

    /**
     * Verify if we can get a lock on all dates we need. If not throw a CampsiteReservationConflictException
     */
    private boolean lockDatesForProcessing(LocalDate startDate, LocalDate endDate) {
        final List<LocalDate> dates = findLocalDateBetween(startDate, endDate);
        return dates.stream()
                .map(date -> availableDateLockRepository.lockAvailableDate(date, Thread.currentThread().getName()))
                .reduce((b1, b2) -> b1 && b2).orElse(false);
    }

    private void unlockDates(LocalDate startDate, LocalDate endDate) {
        findLocalDateBetween(startDate, endDate).stream()
                .forEach(date -> availableDateLockRepository.freeAvailableDate(date, Thread.currentThread().getName()));
    }

    private void checkForAvailability(LocalDate startDate, LocalDate endDate) {
        final List<LocalDate> reservedDates = getReservedDatesBetween(startDate, endDate);
        if (!reservedDates.isEmpty()) {
            throw new CampsiteReservationConflictException(reservedDates);
        }
    }

    private List<LocalDate> getReservedDatesBetween(LocalDate fromInterval, LocalDate toInterval) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(campsiteReservation -> isDayInInterval(campsiteReservation.getStartDate(), fromInterval, toInterval))
                .flatMap(campsiteReservation -> findLocalDateBetween(campsiteReservation.getStartDate(), campsiteReservation.getEndDate()).stream())
                .collect(toList());
    }

    private boolean isDayInInterval(LocalDate day, LocalDate fromInterval, LocalDate toInterval) {
        return (day.isEqual(fromInterval) || day.isAfter(fromInterval)) && day.isBefore(toInterval);
    }

    private List<LocalDate> findLocalDateBetween(LocalDate start, LocalDate end) {
        final long nbDaysBetween = DAYS.between(start, end);
        return LongStream.range(0, nbDaysBetween).mapToObj(offset -> start.plusDays(offset)).collect(Collectors.toList());
    }

    public Iterable<CampsiteReservation> getAllReservation() {
        return repository.findAll();
    }

    public CampsiteReservation updateReservation(String id, CampsiteReservation reservation) throws InterruptedException {
        final CampsiteReservation entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(format("No reservation with id=%s was found", id)));
        if (!entity.getUser().equals(reservation.getUser())) {
            throw new IllegalArgumentException("It's not possible to change the owner of a reservation, only the reservation time can be changed");
        }
        @NotNull final LocalDate startDate = reservation.getStartDate();
        @NotNull final LocalDate endDate = reservation.getEndDate();
        validator.validateReservation(startDate, endDate);
        if(lockDatesForProcessing(startDate, endDate)) {
            try {
                checkForAvailability(startDate, endDate);
                entity.setStartDate(startDate);
                entity.setEndDate(endDate);
                return repository.save(entity);
            } finally {
                unlockDates(startDate, endDate);
            }
        }
        throw new CampsiteReservationConflictException(findLocalDateBetween(startDate, endDate));

    }

    public void delete(String id) {
        final CampsiteReservation entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(format("No reservation with id=%s was found", id)));
        repository.delete(entity);
    }

}
