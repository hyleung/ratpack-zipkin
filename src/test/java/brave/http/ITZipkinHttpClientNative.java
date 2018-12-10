package brave.http;

import brave.ScopedSpan;
import brave.Tracer;
import brave.internal.HexCodec;
import brave.internal.Lists;
import brave.sampler.Sampler;
import brave.test.http.ITHttp;
import io.netty.buffer.UnpooledByteBufAllocator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.*;
import ratpack.http.client.HttpClient;
import ratpack.server.ServerConfig;
import ratpack.test.exec.ExecHarness;
import ratpack.util.Exceptions;
import ratpack.zipkin.ClientTracingInterceptor;
import ratpack.zipkin.TracedParallelBatch;
import ratpack.zipkin.internal.DefaultClientTracingInterceptor;
import ratpack.zipkin.internal.RatpackCurrentTraceContext;
import zipkin2.Span;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ITZipkinHttpClientNative extends ITHttp {

  @Rule
  public MockWebServer server = new MockWebServer();

  private HttpClient client;

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
        ClientTracingInterceptor clientTracingInterceptor = new DefaultClientTracingInterceptor(httpTracing, e);
        client = HttpClient.of(s -> s
          .poolSize(0)
          .requestIntercept(clientTracingInterceptor::request)
          .responseIntercept(clientTracingInterceptor::response)
          .errorIntercept(clientTracingInterceptor::error)
          .byteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
          .maxContentLength(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH));
      });
    });
  }

  @Override @After public void close() throws Exception {
    client.close();
    super.close();
  }

  @Test public void makesChildOfCurrentSpanAsync() throws Exception {
    server.enqueue(new MockResponse());

    ScopedSpan parent = httpTracing.tracing.tracer().startScopedSpan("test");
    harness.yield(e ->
      client
        .get(URI.create("http://localhost:" + server.getPort() + "/foo"))
        .next(r -> parent.finish())
    ).getValueOrThrow();

    RecordedRequest request = server.takeRequest();
    assertThat(request.getHeader("x-b3-traceId"))
      .isEqualTo(parent.context().traceIdString());
    assertThat(request.getHeader("x-b3-parentspanid"))
      .isEqualTo(HexCodec.toLowerHex(parent.context().spanId()));

    assertThat(Arrays.asList(takeSpan(), takeSpan()))
      .extracting(Span::kind)
      .containsOnly(null, Span.Kind.CLIENT);
  }

  @Test public void usesParentFromInvocationTime() throws Exception {
    Tracer tracer = httpTracing.tracing().tracer();
    server.enqueue(new MockResponse().setBodyDelay(300, TimeUnit.MILLISECONDS));
    server.enqueue(new MockResponse());

    harness.run(e -> {
      ScopedSpan parent = tracer.startScopedSpan("test");
      client.get(URI.create("http://localhost:" + server.getPort() + "/item/1"))
        .flatMap(r -> client.get(URI.create("http://localhost:" + server.getPort() + "/item/1")))
        .then(r -> parent.finish());

      ScopedSpan otherSpan = tracer.startScopedSpan("test2");
      try {
        for (int i = 0; i < 2; i++) {
          RecordedRequest request = server.takeRequest();
          assertThat(request.getHeader("x-b3-traceId"))
            .isEqualTo(parent.context().traceIdString());
          assertThat(request.getHeader("x-b3-parentspanid"))
            .isEqualTo(HexCodec.toLowerHex(parent.context().spanId()));
        }
      } finally {
        otherSpan.finish();
      }

    });

    Span parentSpan = takeSpan();
    Span request1Span = takeSpan();
    Span request2Span = takeSpan();
    Span otherSpan = takeSpan();

    // Check we reported 2 in-process spans and 2 RPC client spans
    assertThat(Arrays.asList(parentSpan, request1Span, request2Span, otherSpan))
      .extracting(Span::kind)
      .containsOnly(null, Span.Kind.CLIENT);
  }

}
