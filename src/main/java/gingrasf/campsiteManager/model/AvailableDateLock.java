package gingrasf.campsiteManager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;


@AllArgsConstructor
@NoArgsConstructor(force = true)
@Builder
@Value
/**
 * Simple wrapper entity used to represent a lock on single day while we are processing a reservation. This ensure
 * that only one request is processing a reservation for that specific day.
 */
public class AvailableDateLock {
    /**
     * String representation of a day in ISO-8601. Ex: 2019-01-30
     */
    String date;

    /**
     * String to represent the current owner of a lock.
     */
    String owner;
}
