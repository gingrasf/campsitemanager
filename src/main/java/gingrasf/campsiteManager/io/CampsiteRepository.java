package gingrasf.campsiteManager.io;

import gingrasf.campsiteManager.model.CampsiteReservation;
import gingrasf.campsiteManager.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;


public interface CampsiteRepository extends CrudRepository<CampsiteReservation, String> {

//    default List<CampsiteReservation> getCampsiteReservations() {
//        return asList(CampsiteReservation.builder()
//                                            .id("test")
//                                            .user(User.builder()
//                                                    .email("test@test.com")
//                                                    .fullName("Test User")
//                                                    .build())
//                                            .startDate(LocalDate.now())
//                                            .endDate(LocalDate.now().plusDays(2))
//                                            .build());
//    }
}
