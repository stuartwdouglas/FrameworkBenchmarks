package io.quarkus.benchmark.resource.hibernate;

import io.quarkus.benchmark.model.hibernate.World;
import io.quarkus.benchmark.repository.hibernate.WorldRepository;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.hibernate.FlushMode;
import org.hibernate.Session;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;


@ApplicationScoped
public class DbResource extends BaseResource {

    @Inject
    WorldRepository worldRepository;

    @Route(path = "db", type = Route.HandlerType.BLOCKING)
    public void db(final RoutingContext rc) {
        World world = randomWorldForRead();
        if (world!=null){
            sendJson(rc, world);
        } else {
            handleFail(rc, new IllegalStateException( "No data found in DB. Did you seed the database? Make sure to invoke /createdata once." ));
        }
    }

    @Route(path = "/queries", type = Route.HandlerType.BLOCKING)
    public void queries(final RoutingContext rc) {
        String queries = rc.pathParam("queries");
        final int count = parseQueryCount(queries);
        World[] worlds = randomWorldForRead(count).toArray(new World[0]);
        sendJson(rc, worlds);
    }

    @Route(path = "/updates", type = Route.HandlerType.BLOCKING)
    //Rules: https://github.com/TechEmpower/FrameworkBenchmarks/wiki/Project-Information-Framework-Tests-Overview#database-updates
    //N.B. the benchmark seems to be designed to get in deadlocks when using a "safe pattern" of updating
    // the entity within the same transaction as the one which read it.
    // We therefore need to do a "read then write" while relinquishing the transaction between the two operations, as
    // all other tested frameworks seem to do.
    public void updates(final RoutingContext rc) {
        String queries = rc.pathParam("queries");
        final int count = parseQueryCount(queries);
        try(Session session = worldRepository.openSession()){
            session.setHibernateFlushMode(FlushMode.MANUAL);
            final Collection<World> worlds = randomWorldForUpdate(session, count);
            worlds.forEach( w -> {
                //Read the one field, as required by the following rule:
                // # vi. At least the randomNumber field must be read from the database result set.
                final int previousRead = w.getRandomNumber();
                //Update it, but make sure to exclude the current number as Hibernate optimisations would have us "fail"
                //the verification:
                w.setRandomNumber(randomWorldNumber(previousRead));
            } );
            session.flush();
            sendJson(rc, worlds.toArray(new World[0]));
        }
    }

    @Route(path = "/createdata", type = Route.HandlerType.BLOCKING)
    public void createData(final RoutingContext rc) {
        worldRepository.createData();
        rc.response().setStatusCode(200).end();
    }

    private World randomWorldForRead() {
        return worldRepository.findSingleAndStateless(randomWorldNumber());
    }

    private Collection<World> randomWorldForRead(int count) {
        Set<Integer> ids = new HashSet<>(count);
        int counter = 0;
        while (counter < count) {
            counter += ids.add(Integer.valueOf(randomWorldNumber())) ? 1 : 0;
        }
        return worldRepository.find(ids);
    }

    private Collection<World> randomWorldForUpdate(Session session, int count) {
        Set<Integer> ids = new HashSet<>(count);
        int counter = 0;
        while (counter < count) {
            counter += ids.add(Integer.valueOf(randomWorldNumber())) ? 1 : 0;
        }
        return worldRepository.find(session, ids);
    }

    /**
     * According to benchmark requirements
     * @return returns a number from 1 to 10000
     */
    private int randomWorldNumber() {
        return 1 + ThreadLocalRandom.current().nextInt(10000);
    }


    /**
     * Also according to benchmark requirements, except that in this special case
     * of the update test we need to ensure we'll actually generate an update operation:
     * for this we need to generate a random number between 1 to 10000, but different
     * from the current field value.
     * @param previousRead
     * @return
     */
    private int randomWorldNumber(final int previousRead) {
        //conceptually split the random space in those before previousRead,
        //and those after: this approach makes sure to not affect the random characteristics.
        final int trueRandom = ThreadLocalRandom.current().nextInt(9999) + 2;
        if (trueRandom<=previousRead) {
            //all figures equal or before the current field read need to be shifted back by one
            //so to avoid hitting the same number while not affecting the distribution.
            return trueRandom - 1;
        }
        else {
            //Those after are generated by taking the generated value 2...10000 as is.
            return trueRandom;
        }
    }

    private int parseQueryCount(String textValue) {
        if (textValue == null) {
            return 1;
        }
        int parsedValue;
        try {
            parsedValue = Integer.parseInt(textValue);
        } catch (NumberFormatException e) {
            return 1;
        }
        return Math.min(500, Math.max(1, parsedValue));
    }
}
