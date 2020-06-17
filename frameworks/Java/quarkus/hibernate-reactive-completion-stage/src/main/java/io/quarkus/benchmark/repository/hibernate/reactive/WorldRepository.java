package io.quarkus.benchmark.repository.hibernate.reactive;

import io.quarkus.benchmark.model.hibernate.reactive.World;
import org.hibernate.reactive.stage.Stage;

import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;


@Singleton
public class WorldRepository extends BaseRepository {

    public CompletionStage<World> find(int id) {
        return inSession(session -> singleFind(session, id));
    }

    private static CompletionStage<World> singleFind(final Stage.Session ss, final Integer id) {
        return ss.find(World.class, id);
    }

}
