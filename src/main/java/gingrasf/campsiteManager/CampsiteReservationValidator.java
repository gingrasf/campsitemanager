package gingrasf.campsiteManager;

import gingrasf.campsiteManager.model.User;

import java.time.LocalDate;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;

public class CampsiteReservationValidator {

    public static final int MAX_RESERVATION_DURATION = 3;

    /**
     * Validate is the requested start and end dates are valid for a reservation. If they are not an IllegalArgumentException will be thrown.
     */
    public void validateReservation(LocalDate startDate, LocalDate endDate) {
        final LocalDate today = LocalDate.now();
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Can't make a reservation starting after it ends");
        }
        if (endDate.isEqual(startDate)) {
            throw new IllegalArgumentException("Minimum duration for a reservation is one day. The endDate parameter is exclusive.");
        }
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("Can't make a reservation starting in the past");
        }
        if (startDate.isEqual(today)) {
            throw new IllegalArgumentException("Reservation need to be made at least one day ahead of arrival");
        }
        if (startDate.isAfter(today.plusMonths(1))) {
            throw new IllegalArgumentException("Reservation can only be made up to one month in advance");
        }
        final long reservationDuration = DAYS.between(startDate, endDate);
        if (reservationDuration > MAX_RESERVATION_DURATION) {
            throw new IllegalArgumentException(format("Reservation are only allowed for a maximum of %d", MAX_RESERVATION_DURATION));
        }
    }

    /**
     * Validate that the user is valid for a reservation. If it is not an IllegalArgumentException will be thrown.
     */
    public void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Reservation must have a user.");
        }
    }
}
