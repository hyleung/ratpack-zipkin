package brave.http;

import brave.sampler.Sampler;
import brave.test.http.ITHttpAsyncClient;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ratpack.exec.Promise;
import ratpack.http.client.HttpClient;
import ratpack.server.ServerConfig;
import ratpack.test.exec.ExecHarness;
import ratpack.util.Exceptions;
import ratpack.zipkin.ClientTracingInterceptor;
import ratpack.zipkin.internal.DefaultClientTracingInterceptor;
import ratpack.zipkin.internal.RatpackCurrentTraceContext;

import java.io.IOException;
import java.net.URI;

public class ITZipkinHttpClient extends ITHttpAsyncClient<HttpClient> {

  private static ExecHarness harness;

  @BeforeClass
  public static void beforeAll() {
    harness = ExecHarness.harness();
  }

  @AfterClass
  public static void afterAll() {
    harness.close();
  }

  @Before public void setup() {
    Exceptions.uncheck(() -> {
      harness.run(e -> {
        currentTraceContext = new RatpackCurrentTraceContext(() -> e);
        httpTracing = HttpTracing.create(tracingBuilder(Sampler.ALWAYS_SAMPLE).build());
        client = newClient(server.getPort());
      });
    });

  }

  @Override protected HttpClient newClient(int port) {
    return Exceptions.uncheck(() -> harness.yield(e -> {
      ClientTracingInterceptor clientTracingInterceptor = new DefaultClientTracingInterceptor(httpTracing, e);
      return Promise.value(HttpClient.of(s -> s
        .poolSize(0)
        .requestIntercept(clientTracingInterceptor::request)
        .responseIntercept(clientTracingInterceptor::response)
        .errorIntercept(clientTracingInterceptor::error)
        .byteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
        .maxContentLength(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH)));
    }).getValue());
  }

  @Override protected void closeClient(HttpClient client) throws IOException {
    if (client != null) {
      client.close();
    }
  }

  @Override protected void get(HttpClient client, String pathIncludingQuery) throws Exception {
    harness.yield(e ->
      client.get(URI.create(url(pathIncludingQuery)))
    );
    // small delay to ensure the response interceptor is invoked before this method exits.
    Thread.sleep(1);
  }

  @Override protected void post(HttpClient client, String pathIncludingQuery, String body) throws Exception {
    harness.yield(e ->
      client.post(URI.create(url(pathIncludingQuery)), (request ->
        request.body(b -> b.text(body))
      ))
    );
    // Small delay to ensure the response interceptor is invoked before this method exits.
    // This only really effects redirect trace tests.  All other tests work fine without it.
    Thread.sleep(1);
  }

  @Override @Test(expected = AssertionError.class)
  public void reportsServerAddress() throws Exception { // doesn't know the remote address
    super.reportsServerAddress();
  }

  @Override
  protected void getAsync(HttpClient client, String pathIncludingQuery) throws Exception {
    harness.yield( e -> client.requestStream(URI.create(url(pathIncludingQuery)), r -> r.get()));
  }

  @Override @Test
  public void usesParentFromInvocationTime() throws Exception {
    harness.run(e -> super.usesParentFromInvocationTime());
  }

}
