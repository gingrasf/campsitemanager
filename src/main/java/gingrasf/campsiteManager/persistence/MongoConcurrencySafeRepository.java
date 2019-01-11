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
        final AvailableDateLock existingLock = mongoOperations
                                                    .findAndModify(buildQueryForSpecificDate(date),
                                                                    buildUpsertQuery(date, owner),
                                                                    options().upsert(true).returnNew(false),
                                                                    AvailableDateLock.class);
        return existingLock == null;
    }

    /**
     * Build an upsert query that will insert the date is it's not present, is it is it will update it.
     * The owner will only be added on insert request, i.e. if the record already exist, there won't be any change to it since we'll only update the date to itself.
     */
    private Update buildUpsertQuery(LocalDate date, String owner) {
        return Update.update("date", date.toString())
                .setOnInsert("owner", owner);
    }

    private Query buildQueryForSpecificDate(LocalDate date) {
        return Query.query(where("date").is(date.toString()));
    }

    @Override
    public void freeAvailableDate(LocalDate date, String owner) {
        mongoOperations.remove(buildQueryForSpecificDate(date), AvailableDateLock.class);
    }
}
