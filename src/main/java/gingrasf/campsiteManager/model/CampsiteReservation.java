package gingrasf.campsiteManager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.data.annotation.Id;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;


@AllArgsConstructor
@NoArgsConstructor(force = true)
@Builder
@Value
@Validated
public class CampsiteReservation implements Serializable {

    @Id
    /**
     * Unique id representing a completed reservation
     */
    String id;

    @NotNull
    @Valid
    /**
     * The user that made the reservation
     */
    User user;


    @NonFinal
    @Setter
    @NotNull
    /**
     * The start of the reservation. Inclusive
     */
    LocalDate startDate;

    @NonFinal
    @Setter
    @NotNull
    /**
     * The end of the reservation. Exclusive.
      */
    LocalDate endDate;
}
