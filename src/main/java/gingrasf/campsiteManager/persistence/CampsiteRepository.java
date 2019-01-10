package gingrasf.campsiteManager.persistence;

import gingrasf.campsiteManager.model.CampsiteReservation;
import org.springframework.data.repository.CrudRepository;


public interface CampsiteRepository extends CrudRepository<CampsiteReservation, String> {

}
