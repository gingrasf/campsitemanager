package gingrasf.campsiteManager.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Builder
@Value
public class CampsiteAvailability {
    /**
     * List of dates available for the campsite for the searched period.
     */
    List<LocalDate> availableDates;

    /**
     * The start of the search period, this is inclusive
     */
    LocalDate searchPeriodStart;

    /**
     * The end of the search period, this is exclusive.
     */
    LocalDate searchPeriodEnd;
}
