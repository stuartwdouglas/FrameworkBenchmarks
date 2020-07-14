package io.quarkus.benchmark.repository.pgclient;

import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.SqlClient;

import java.util.concurrent.atomic.AtomicInteger;

class PgClients {
//    private static final int POOL_SIZE = 4;

    private AtomicInteger pool_size = new AtomicInteger(1);

    private ThreadLocal<SqlClient> sqlClient = new ThreadLocal<>();
    private ThreadLocal<PgPool> pool = new ThreadLocal<>();
    private PgClientFactory pgClientFactory;

	// for ArC
	public PgClients() {
	}

	public PgClients(PgClientFactory pgClientFactory, Integer pool_size) {
	    this.pgClientFactory = pgClientFactory;
        this.pool_size.set(pool_size);
    }

    SqlClient getClient() {
        SqlClient ret = sqlClient.get();
        if(ret == null) {
            ret = pgClientFactory.sqlClient(pool_size.get());
            sqlClient.set(ret);
        }
		return ret;
	}

//	synchronized PgPool getPool() {
//        PgPool ret = pool.get();
//        if(ret == null) {
//            ret = pgClientFactory.sqlClient(POOL_SIZE);
//            pool.set(ret);
//        }
//        return ret;
//	}
}