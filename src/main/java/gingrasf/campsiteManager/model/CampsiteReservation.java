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
    String id;
    @NotNull
    @Valid
    User user;
    @NonFinal
    @Setter
    @NotNull
    LocalDate startDate;
    @NonFinal
    @Setter
    @NotNull
    LocalDate endDate;
}
