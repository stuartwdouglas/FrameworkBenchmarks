package hello;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.Headers;
import org.xnio.XnioIoThread;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static hello.HelloWebServer.JSON_UTF8;

/**
 * Handles the JSON test.
 */
final class JsonHandler implements HttpHandler {
  private final ObjectMapper objectMapper;

  private static final Map<XnioIoThread, Long> threadTime = new ConcurrentHashMap<>();
    private final AtomicBoolean first = new AtomicBoolean(true);

  public JsonHandler(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
      if(first.compareAndSet(true, false)) {
          exchange.getIoThread().executeAtInterval(new Runnable() {
              @Override
              public void run() {

                  System.out.println("==================");
                  System.out.println("STATS: ");
                  System.out.println("==================");
                  for(Map.Entry<XnioIoThread, Long> entry : threadTime.entrySet()) {
                      System.out.println(entry.getKey() + " \t" + entry.getValue());
                  }
              }
          }, 1, TimeUnit.SECONDS);
      }
      final long start = System.nanoTime();
    exchange.getResponseHeaders().put(
        Headers.CONTENT_TYPE, JSON_UTF8);
      exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
          @Override
          public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
              final long taken = System.nanoTime() - start;
              Long existing = threadTime.get(exchange.getIoThread());
              if(existing == null) {
                  threadTime.put(exchange.getIoThread(), taken);
              } else {
                  threadTime.put(exchange.getIoThread(), taken + existing);
              }
              nextListener.proceed();
          }
      });
    exchange.getResponseSender().send(ByteBuffer.wrap(
            objectMapper.writeValueAsBytes(
                    Collections.singletonMap("message", "Hello, World!"))));
  }
}
