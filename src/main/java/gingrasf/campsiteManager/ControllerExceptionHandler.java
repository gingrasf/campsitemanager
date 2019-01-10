package gingrasf.campsiteManager;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { IllegalArgumentException.class })
    public ResponseEntity<String> handleBadRequest(Exception ex) {
        return new ResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = { CampsiteReservationConflictException.class })
    public ResponseEntity<String> handleConflict(Exception ex) {
        return new ResponseEntity(ex.getMessage(), HttpStatus.CONFLICT);
    }


}
