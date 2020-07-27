package io.quarkus.benchmark.resource.pgclient;

import io.quarkus.benchmark.model.World;
import io.quarkus.benchmark.repository.pgclient.WorldRepository;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.concurrent.ThreadLocalRandom;


@Path("/db")
public class DbResource extends BaseResource {

    @Inject
    WorldRepository worldRepository;

    @GET
    public Uni<World> db(RoutingContext rc) {
        return randomWorld();
    }

    private Uni<World> randomWorld() {
        return worldRepository.find(randomWorldNumber());
    }

    private int randomWorldNumber() {
        return 1 + ThreadLocalRandom.current().nextInt(10000);
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