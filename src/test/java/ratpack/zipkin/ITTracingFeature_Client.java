package ratpack.zipkin;

import brave.http.ITHttpClient;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ratpack.http.client.HttpClient;

public class ITTracingFeature_Client extends ITHttpClient<HttpClient> {

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override protected HttpClient newClient(int port) {
        //return new ZipkinHttpClientImpl(HttpClient.of(noIdea), httpTracing);
        return null;
    }

    @Override protected void closeClient(HttpClient client) throws IOException {
        client.close();
        executor.shutdown();
    }

    @Override protected void get(HttpClient client, String pathIncludingQuery) throws IOException {
        //client.get(URI.create(url(pathIncludingQuery)))..noidea..close()
    }

    @Override protected void post(HttpClient client, String pathIncludingQuery, String body)
            throws Exception {
        // no idea
    }

    @Override protected void getAsync(HttpClient client, String pathIncludingQuery) throws Exception {
        client.get(URI.create(url(pathIncludingQuery))).fork(); //? maybe?
    }
}
