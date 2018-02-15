package brave.http;

import brave.sampler.Sampler;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.io.IOException;
import java.net.URI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ratpack.exec.Execution;
import ratpack.http.client.HttpClient;
import ratpack.server.ServerConfig;
import ratpack.test.exec.ExecHarness;
import ratpack.util.Exceptions;
import ratpack.zipkin.internal.RatpackCurrentTraceContext;
import ratpack.zipkin.internal.ZipkinHttpClientImpl;

public class ITZipkinHttpClientImpl extends ITHttpAsyncClient<HttpClient> {

    private static ExecHarness harness;

    @BeforeClass
    public static void beforeAll() {
        harness = ExecHarness.harness();
    }

    @AfterClass
    public static void afterAll() {
        harness.close();
    }

    void harnessSetup(Execution execution) {
        currentTraceContext = new RatpackCurrentTraceContext(() -> execution);
        httpTracing = HttpTracing.create(tracingBuilder(Sampler.ALWAYS_SAMPLE).build());
        client = newClient(server.getPort());
    }

    @Override protected HttpClient newClient(int port) {
        return Exceptions.uncheck(() -> new ZipkinHttpClientImpl( HttpClient.of(s -> s
            .poolSize(0)
            .byteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
            .maxContentLength(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH)
        ), httpTracing));
    }

    @Override protected void closeClient(HttpClient client) throws IOException {
        client.close();
    }

    @Override protected void get(HttpClient client, String pathIncludingQuery) throws Exception {
        harness.yield(e -> client.get(URI.create(url(pathIncludingQuery)))).getValueOrThrow();
    }

    @Override protected void post(HttpClient client, String pathIncludingQuery, String body)
        throws Exception {
        harness.yield(e ->
            client.post(URI.create(url(pathIncludingQuery)), (request ->
                request.body(b -> b.text(body))
            ))
        ).getValueOrThrow();
    }

    @Override protected void getAsync(HttpClient client, String pathIncludingQuery)
        throws Exception {
        harness.yield(e -> client.requestStream(URI.create(url(pathIncludingQuery)), r -> {
        })).getValueOrThrow();
    }

    @Override @Test(expected = AssertionError.class)
    public void reportsServerAddress() throws Exception { // doesn't know the remote address
        super.reportsServerAddress();
    }

    @Test
    @Override public void makesChildOfCurrentSpan() throws Exception {
        harness.run( e -> {
            harnessSetup(e);
            super.makesChildOfCurrentSpan();
        });
    }

    @Test
    @Override public void redirect() throws Exception {
        harness.run( e -> {
            harnessSetup(e);
            super.redirect();
        });
    }

    @Test
    @Override public void usesParentFromInvocationTime() throws Exception {
        harness.run( e -> {
            harnessSetup(e);
            super.usesParentFromInvocationTime();
        });
    }

    @Test
    @Override public void propagatesExtra_unsampledTrace() throws Exception {
        harness.run( e -> {
            harnessSetup(e);
            super.propagatesExtra_unsampledTrace();
        });
    }

    @Test
    @Override public void propagatesExtra_newTrace() throws Exception {
        harness.run( e -> {
            harnessSetup(e);
            super.propagatesExtra_newTrace();
        });
    }
}
