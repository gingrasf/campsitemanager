package gingrasf.campsiteManager.persistence;

import java.time.LocalDate;

public interface AvailableDateLockRepository {

    /**
     * @return true is the lock was acquired correctly for this date, false otherwise.
     */
    boolean lockAvailableDate(LocalDate date, String owner);

    void freeAvailableDate(LocalDate date, String owner);
}
