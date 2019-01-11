package gingrasf.campsiteManager.config;

import gingrasf.campsiteManager.CampsiteReservationValidator;
import gingrasf.campsiteManager.CampsiteService;
import gingrasf.campsiteManager.persistence.AvailableDateLockRepository;
import gingrasf.campsiteManager.persistence.CampsiteRepository;
import gingrasf.campsiteManager.persistence.MongoConcurrencySafeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CampsiteManagerConfig {

    @Bean
    public CampsiteReservationValidator validator() {
        return new CampsiteReservationValidator();
    }

    @Bean
    public AvailableDateLockRepository availableDateLockRepository() {
        return new MongoConcurrencySafeRepository();
    }

    @Bean
    public CampsiteService campsiteService(CampsiteRepository campsiteRepository, AvailableDateLockRepository availableDateLockRepository, CampsiteReservationValidator validator) {
        return new CampsiteService(campsiteRepository, availableDateLockRepository, validator);
    }


}
