package gingrasf.campsiteManager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@NoArgsConstructor(force = true)
@Builder
@Value
public class AvailableDateLock {
    String date;
    String owner;
}
