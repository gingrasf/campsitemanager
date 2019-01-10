package gingrasf.campsiteManager.config;

import gingrasf.campsiteManager.CampsiteReservationValidator;
import gingrasf.campsiteManager.CampsiteService;
import gingrasf.campsiteManager.io.CampsiteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CampsiteManagerConfig {

    @Bean
    public CampsiteReservationValidator validator() {
        return new CampsiteReservationValidator();
    }

    @Bean
    public CampsiteService campsiteService(CampsiteRepository campsiteRepository, CampsiteReservationValidator validator) {
        return new CampsiteService(campsiteRepository, validator);
    }


}
