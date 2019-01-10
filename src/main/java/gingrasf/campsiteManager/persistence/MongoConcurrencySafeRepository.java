package gingrasf.campsiteManager.persistence;

import gingrasf.campsiteManager.model.AvailableDateLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;

public class MongoConcurrencySafeRepository implements AvailableDateLockRepository {

    @Autowired
    MongoOperations mongoOperations;


    @Override
    public boolean lockAvailableDate(LocalDate date, String owner) {
        final AvailableDateLock existingLock = mongoOperations.findAndModify(Query.query(where("date").is(date.toString())), Update.update("date", date.toString()).setOnInsert("owner", owner), options().upsert(true), AvailableDateLock.class);
        return existingLock == null;
    }

    @Override
    public void freeAvailableDate(LocalDate date, String owner) {
        mongoOperations.remove(Query.query(where("date").is(date.toString())), AvailableDateLock.class);
    }
}
