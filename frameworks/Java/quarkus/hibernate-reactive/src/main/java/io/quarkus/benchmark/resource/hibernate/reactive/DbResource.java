package io.quarkus.benchmark.resource.hibernate.reactive;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hibernate.FlushMode;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.benchmark.model.hibernate.reactive.World;
import io.quarkus.benchmark.repository.hibernate.reactive.WorldRepositoryMutiny;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;


@ApplicationScoped
public class DbResource extends BaseResource {

    @Inject
    WorldRepositoryMutiny worldRepository;

    @Route(path = "db")
    public void db(RoutingContext rc) {
        randomWorld().subscribe().with(world -> sendJson(rc, world),
                                       t -> handleFail(rc, t));
    }

    @Route(path = "queries")
    public void queries(RoutingContext rc) {
        var queries = rc.request().getParam("queries");
        worldRepository.inSession(session -> randomWorldForRead(session, parseQueryCount(queries)))
        .subscribe().with(list -> sendJson(rc, list),
                          t -> handleFail(rc, t));
    }

    //Rules: https://github.com/TechEmpower/FrameworkBenchmarks/wiki/Project-Information-Framework-Tests-Overview#database-updates
    //N.B. the benchmark seems to be designed to get in deadlocks when using a "safe pattern" of updating
    // the entity within the same transaction as the one which read it.
    // We therefore need to do a "read then write" while relinquishing the transaction between the two operations, as
    // all other tested frameworks seem to do.
    @Route(path = "updates")
    public void updates(RoutingContext rc) {
        var queries = rc.request().getParam("queries");
        worldRepository.inSession(session -> {
            // FIXME: not supported
            //          session.setJdbcBatchSize(worlds.size());
            session.setFlushMode(FlushMode.MANUAL);

            var worlds = randomWorldForRead(session, parseQueryCount(queries));
            return worlds.flatMap(worldsCollection -> {
                worldsCollection.forEach( w -> {
                    //Read the one field, as required by the following rule:
                    // # vi. At least the randomNumber field must be read from the database result set.
                    final int previousRead = w.getRandomNumber();
                    //Update it, but make sure to exclude the current number as Hibernate optimisations would have us "fail"
                    //the verification:
                    w.setRandomNumber(randomWorldNumber(previousRead));
                } );
                
                return worldRepository.update(session, worldsCollection);
            });
        }).subscribe().with(list -> sendJson(rc, list),
                            t -> handleFail(rc, t));
    }

    private Uni<Collection<World>> randomWorldForRead(Mutiny.Session session, int count) {
        Set<Integer> ids = new HashSet<>(count);
        int counter = 0;
        while (counter < count) {
            counter += ids.add(Integer.valueOf(randomWorldNumber())) ? 1 : 0;
        }
        return worldRepository.find(session, ids);
    }

    @Route(path = "createdata")
    public void createData(RoutingContext rc) {
        worldRepository.createData().subscribe().with(v -> rc.response().end("Data created"),
                                                      t -> handleFail(rc, t));
    }

    private Uni<World> randomWorld() {
        int i = randomWorldNumber();
        return worldRepository.find(i);
    }

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