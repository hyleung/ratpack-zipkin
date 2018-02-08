package brave.http;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import ratpack.guice.Guice;
import ratpack.http.Status;
import ratpack.test.embed.EmbeddedApp;
import ratpack.zipkin.ServerTracingModule;

import java.net.URI;

public class ITServerTracingModule extends ITHttpServer {
  private EmbeddedApp app;
  @Override
  protected void init() throws Exception {
    Module tracingModule = Modules
        .override(new ServerTracingModule())
        .with(binder -> binder.bind(HttpTracing.class).toInstance(httpTracing));
    app = EmbeddedApp
        .of(server ->
            server.registry(Guice.registry(binding -> binding.module(tracingModule)))
                  .handlers(chain -> chain
                      .get("/foo", ctx -> ctx.getResponse().send("bar"))
                      .get("/badrequest", ctx -> ctx.getResponse().status(400).send())
                      .get("/child", ctx -> {
                        HttpTracing httpTracing = ctx.get(HttpTracing.class);
                        httpTracing.tracing().tracer().nextSpan().name("child").start().finish();
                        ctx.getResponse().send("happy");
                      })
                      .get("/extra", ctx -> ctx.getResponse().send("joey"))
                      .all(ctx -> ctx.getResponse().status(500).send()))
        );
  }

  @Override
  protected String url(final String path) {
    URI uri = app.getAddress();
    return String
        .format("%s://%s:%d/%s", uri.getScheme(), "127.0.0.1", uri.getPort(), path);
  }

}
