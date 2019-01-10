package gingrasf.campsiteManager;

import gingrasf.campsiteManager.model.CampsiteAvailability;
import gingrasf.campsiteManager.model.CampsiteReservation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

import static java.util.Optional.ofNullable;

@RestController
@RequestMapping("/campsite")
public class CampsiteManagerController {

    private final CampsiteService campsiteService;


    public CampsiteManagerController(CampsiteService campsiteService) {
        this.campsiteService = campsiteService;
    }

    @GetMapping("/availability")
    public CampsiteAvailability getAvailability(@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(name = "until", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until) {
        return campsiteService.getAvailabilityBetween(ofNullable(from).orElse(LocalDate.now()), ofNullable(until).orElse(LocalDate.now().plusMonths(1)));
    }

    @PutMapping("/reservation")
    @Transactional
    public String createReservation(@Valid @RequestBody CampsiteReservation reservation) throws InterruptedException {
        return campsiteService.createReservation(reservation.getUser(), reservation.getStartDate(), reservation.getEndDate()).getId();
    }
    @GetMapping("/reservation")
    public Iterable<CampsiteReservation> getAllReservations() {
        return campsiteService.getAllReservation();
    }

    @DeleteMapping("/reservation/{id}")
    public void delete(@PathVariable("id") String id) {
        campsiteService.delete(id);
    }

    @PostMapping("/reservation/{id}")
    public CampsiteReservation update(@PathVariable("id") String id, @Valid @RequestBody CampsiteReservation reservation) throws InterruptedException {
        return campsiteService.updateReservation(id, reservation);
    }
}
