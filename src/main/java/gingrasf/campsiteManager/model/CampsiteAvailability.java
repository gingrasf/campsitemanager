package gingrasf.campsiteManager.model;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Builder
@Value
public class CampsiteAvailability {
    List<LocalDate> availableDates;
    LocalDate searchPeriodStart;
    LocalDate searchPeriodEnd;
}
