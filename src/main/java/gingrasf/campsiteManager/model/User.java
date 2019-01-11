package gingrasf.campsiteManager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;


@AllArgsConstructor
@NoArgsConstructor(force = true)
@Builder
@Value
@Validated
public class User implements Serializable {

    @Id
    @Email
    @NotEmpty
    /**
     * A valid email address to reach the user
     */
    String email;

    @NotEmpty
    /**
     * The full name of the user
     */
    String fullName;
}
