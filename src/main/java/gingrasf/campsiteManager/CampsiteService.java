package gingrasf.campsiteManager;

import gingrasf.campsiteManager.io.CampsiteRepository;
import gingrasf.campsiteManager.model.CampsiteAvailability;
import gingrasf.campsiteManager.model.CampsiteReservation;
import gingrasf.campsiteManager.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toList;

public class CampsiteService {


    private final CampsiteRepository repository;
    private final CampsiteReservationValidator validator;
    private final ReentrantLock lock = new ReentrantLock();

    public CampsiteService(CampsiteRepository repository, CampsiteReservationValidator validator) {
        this.repository = repository;
        this.validator = validator;
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

        if (lock.tryLock(1, TimeUnit.SECONDS)) {
            try {
                checkForConflicts(startDate, endDate);
                final CampsiteReservation reservation = CampsiteReservation.builder()
                        .id(UUID.randomUUID().toString())
                        .startDate(startDate)
                        .endDate(endDate)
                        .user(user)
                        .build();
                return repository.save(reservation);
            } finally {
                lock.unlock();
            }
        }
        throw new TooManyConcurrentRequestException("There's too much load on the server at the moment, please try your request again later.");
    }

    private void checkForConflicts(LocalDate startDate, LocalDate endDate) {
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
        validator.validateReservation(reservation.getStartDate(), reservation.getEndDate());
        if (lock.tryLock(1, TimeUnit.SECONDS)) {
            try {
                checkForConflicts(reservation.getStartDate(), reservation.getEndDate());
                entity.setStartDate(reservation.getStartDate());
                entity.setEndDate(reservation.getEndDate());
                return repository.save(entity);
            } finally {
                lock.unlock();
            }
        }
        throw new TooManyConcurrentRequestException("There's too much load on the server at the moment, please try your request again later.");

    }

    public void delete(String id) {
        final CampsiteReservation entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(format("No reservation with id=%s was found", id)));
        repository.delete(entity);
    }

}
