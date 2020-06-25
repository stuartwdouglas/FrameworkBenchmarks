package io.quarkus.benchmark.resource.pgclient;

import io.quarkus.benchmark.model.World;
import io.quarkus.benchmark.repository.pgclient.WorldRepository;
import io.quarkus.vertx.web.Route;
import io.reactivex.Single;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.ThreadLocalRandom;


@ApplicationScoped
public class DbResource extends BaseResource {

    @Inject
    WorldRepository worldRepository;

    @Route(path = "db")
    public void db(RoutingContext rc) {
        randomWorld().subscribe(world -> sendJson(rc, world));
    }

//    @Route(path = "queries")
//    public void queries(RoutingContext rc) {
//        var queries = rc.request().getParam("queries");
//        var worlds = new Uni[parseQueryCount(queries)];
//        var ret = new World[worlds.length];
//        Arrays.setAll(worlds, i -> {
//            return randomWorld().map(w -> ret[i] = w);
//        });
//
//        Uni.combine().all().unis(worlds)
//        .combinedWith(v -> Arrays.asList(ret))
//        .subscribe().with(list -> sendJson(rc, list),
//                          t -> handleFail(rc, t));
//    }
//
//    @Route(path = "updates")
//    public void updates(RoutingContext rc) {
//        var queries = rc.request().getParam("queries");
//        var worlds = new Uni[parseQueryCount(queries)];
//        var ret = new World[worlds.length];
//        Arrays.setAll(worlds, i -> {
//            return randomWorld().map(w -> {
//                w.setRandomNumber(randomWorldNumber());
//                ret[i] = w;
//                return w;
//            });
//        });
//
//        Uni.combine().all().unis(worlds)
//        .combinedWith(v -> null)
//        .flatMap(v -> worldRepository.update(ret))
//        .map(v -> Arrays.asList(ret))
//        .subscribe().with(list -> sendJson(rc, list),
//                          t -> handleFail(rc, t));
//    }

    private Single<World> randomWorld() {
        return worldRepository.findSingle(randomWorldNumber());
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