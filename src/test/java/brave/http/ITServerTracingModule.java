package brave.http;

import brave.test.http.ITHttpServer;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import ratpack.exec.Promise;
import ratpack.guice.Guice;
import ratpack.test.embed.EmbeddedApp;
import ratpack.zipkin.ServerTracingModule;

import java.io.IOException;
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
                      .options("/", ctx -> ctx.getResponse().send(""))
                      .get("/foo", ctx -> ctx.getResponse().send("bar"))
                      .get("/async", ctx ->
                          Promise.async(f -> f.success("bar")).then(ctx::render)
                      )
                      .get("/badrequest", ctx -> ctx.getResponse().status(400).send())
                      .get("/child", ctx -> {
                        HttpTracing httpTracing = ctx.get(HttpTracing.class);
                        httpTracing.tracing().tracer().nextSpan().name("child").start().finish();
                        ctx.getResponse().send("happy");
                      })
                      .get("/items/:itemId", ctx ->
                          ctx.getResponse().send(ctx.getPathTokens().get("itemId"))
                      )
                      .prefix("/nested", nested -> nested.get("items/:itemId", ctx ->
                          ctx.getResponse().send(ctx.getPathTokens().get("itemId"))
                      ))
                      .get("/extra", ctx -> ctx.getResponse().send("joey"))
                      .get("/exception", ctx -> {
                        throw new IOException();
                      })
                      .get("/exceptionAsync",
                          ctx -> Promise.async((f) -> f.error(new IOException())).then(ctx::render)
                      )
                      .all(ctx -> ctx.getResponse().status(404).send()))
        );
  }

  @Override
  protected String url(final String path) {
    URI uri = app.getAddress();
    return String
        .format("%s://%s:%d/%s", uri.getScheme(), "127.0.0.1", uri.getPort(), path);
  }

}
