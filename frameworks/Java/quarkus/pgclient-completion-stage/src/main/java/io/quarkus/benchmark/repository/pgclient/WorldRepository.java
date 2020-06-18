package io.quarkus.benchmark.repository.pgclient;

import io.quarkus.benchmark.model.World;
import io.reactivex.Single;
import io.vertx.reactivex.impl.AsyncResultSingle;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.Tuple;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class WorldRepository {

    @Inject
    PgClients clients;


    public Single<World> find(int id) {

        return
                AsyncResultSingle.<RowSet<Row>>toSingle(handler -> {
                    clients.getClient().preparedQuery("SELECT id, randomNumber FROM World WHERE id = $1").execute(Tuple.of(id), handler);
                }).map(rowset -> {
                    Row row = rowset.iterator().next();
                    return new World(row.getInteger(0), row.getInteger(1));
                });
    }


    public Single<World> findSingle(int id) {
        return clients.getClient().preparedQuery("SELECT id, randomNumber FROM World WHERE id = $1").rxExecute(Tuple.of(id)).map(rowset -> {
            Row row = rowset.iterator().next();
            return new World(row.getInteger(0), row.getInteger(1));
        });
    }

//    public Uni<Void> update(World[] worlds) {
//        Arrays.sort(worlds);
//        List<Tuple> args = new ArrayList<>(worlds.length);
//        for(World world : worlds) {
//            args.add(Tuple.of(world.getId(), world.getRandomNumber()));
//        }
//        return clients.getPool().preparedQuery("UPDATE World SET randomNumber = $2 WHERE id = $1").executeBatch(args)
//                .map(v -> null);
//    }
}
