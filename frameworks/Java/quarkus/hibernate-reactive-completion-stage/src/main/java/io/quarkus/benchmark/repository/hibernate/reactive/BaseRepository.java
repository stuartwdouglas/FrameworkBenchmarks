package io.quarkus.benchmark.repository.hibernate.reactive;

import org.hibernate.reactive.stage.Stage;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class BaseRepository {
    @Inject
    protected Stage.SessionFactory sf;

    public <T> CompletionStage<T> inSession(Function<Stage.Session, CompletionStage<T>> work){
        return sf.withSession(session -> {
            return work.apply(session);
        });
    }

}
