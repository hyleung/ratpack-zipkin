# ratpack-zipkin

![Build status](https://travis-ci.org/hyleung/ratpack-zipkin.svg?branch=master) [![codecov.io](http://codecov.io/github/hyleung/ratpack-zipkin/coverage.svg?branch=master)](http://codecov.io/github/hyleung/ratpack-zipkin?branch=master)

[Zipkin](https://twitter.github.io/zipkin/index.html) support for [Ratpack](http://www.ratpack.io).

Uses [Brave](https://github.com/openzipkin/brave) for the underlying Zipkin support.

## Getting Started

### Zipkin

First you'll need to (obviously) get Zipkin up and running. The quickest way to do this is using Docker and `docker-compose`.

Clone [docker-zipkin](https://github.com/openzipkin/docker-zipkin)

Start the Zipkin stack by running;

```
docker-compose up
```
## Local development

To build and install into your local environment:

```
./mvnw clean install
```

## Snapshot Releases

Using Gradle:

```
repositories {
    mavenCentral()
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}
```

Using Maven:

```
<repositories>
  <repository>
    <id>snapshots-repo</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <releases><enabled>false</enabled></releases>
    <snapshots><enabled>true</enabled></snapshots>
  </repository>
</repositories>
```

## Ratpack-zipkin V2 

Version 2 of this library incorporates Brave 4.x.

### v2 Binaries

Using Gradle:

```
compile 'com.github.hyleung:ratpack-zipkin:2.3.0'
```

Using Maven:

```
<dependency>
  <groupId>com.github.hyleung</groupId>
  <artifactId>ratpack-zipkin</artifactId>
  <version>2.3.0</version>
</dependency>
```

### v2 Usage

#### Server Spans

The minimal configuration for enabling tracing of server spans (SR/SS):

```
RatpackServer.start(server -> server
    .serverConfig(config -> config.port(serverPort))
    .registry(Guice.registry(binding -> binding
        .module(ServerTracingModule.class, config -> {
          config
              .serviceName("ratpack-demo")
              .sampler(Sampler.ALWAYS_SAMPLE)
              .spanReporter(aSpanReporter);
        })
    ))
    .handlers(chain -> chain
       ... 
    )
);
```

Where `aSpanReporter` is some instance of `zipkin.reporter.SpanReporter` (e.g. `Reporter.CONSOLE`).

By default, the tracing module uses the Brave HTTP's `HttpServerParser` for parsing HTTP requests
into spans. To customize this behaviour (e.g. add tags based on some data in the request), you'll 
need to subclass `HttpServerParser` and configure the module to use the custom parser.

Span names can be customized by configuring `SpanNameProvider`:

```
config.spanNameProvider((request,pathBindingOpt) -> pathBindingOpt
    .map(pathBinding -> pathBinding.getDescription())
    .orElse(request.getPath())) )
```

Note that due to some Ratpack implementation details, the `PathBinding` may not be present in some edge cases (e.g. if
for some reason an error occurs and no response is sent) - hence the `Optional` type.

#### Client Spans

Client span tracing, for the most part, works the same in v2 as it did in v1. To trace HTTP client spans, use the `@Zipkin` 
annotation to inject a Zipkin instrumented implementation of the Ratpack `HttpClient`.

e.g.


```
@Inject
@Zipkin
HttpClient client
...
client.get(new URI("http://example.com", requestSpec -> ...))
    ... 
```

As with v1, both field and constructor injection will work.

One area where v2 client span tracing differs from v1 is the way in which HTTP requests are parsed into spans. As with 
server spans, by default the tracing module will use Brave HTTP to parse the HTTP request into spans (`HttpClientParser`).
Again, to customize this behaviour, you'll need to extends the Brave HTTP class, and configure the module to use the custom
parser.

#### Nested Spans

This is a feature that we pretty much get "for free" by moving to Brave 4 - and allows you to nest spans. Since it is just
using functionality provided by Brave, there's nothing really Ratpack specific about it. In order to use this feature,
you'll need to `@Inject` a reference to an instance of `brave.http.HttpTracing`. Using that object, you'll be able to
create and start child spans.

```
public class NestedSpanHandler implements Handler {
  @Inject
  private HttpTracing httpTracing;

  @Override
  public void handle(final Context ctx) throws Exception {
    Tracer tracer = httpTracing.tracing().tracer();
    Span child_1 = tracer.newChild(tracer.currentSpan().context()).name("child_1");
    child_1.start();
    try (Tracer.SpanInScope spanInScope_1 = tracer.withSpanInScope(child_1)) {
      Span child_2 = tracer.newChild(child_1.context()).name("child_2");
      child_2.start();
      try (Tracer.SpanInScope spanInScope_2 = tracer.withSpanInScope(child_2)) {
        child_2.finish();
        child_1.finish();
        ctx.getResponse().send("Nested Spans!.");
      }
    }
  }
}
```

One thing to watch out for, `Tracer.SpanInScope` implements `Closable`, which means you can use `try-with-resources` in some
cases. In situations where there is async work going on (e.g. a nested HTTP call), you will probably have to deal with the
closing of each `SpanInScope` yourself.


#### Traced Parallel Batches

`TracedParallelBatch` provides some factory methods that can be used to create instances of `ParallelBatch`, with a given
parent trace context.

Example:

```
ParallelBatch<Integer> batch
    = TracedParallelBatch.of(
    client.get(new URI("http://foo.com"))
          .map(ReceivedResponse::getStatusCode),
    client.get(new URI("http://bar.com"))
          .map(ReceivedResponse::getStatusCode),
    client.get(new URI("http://bazz.com"))
          .map(ReceivedResponse::getStatusCode))
                          .withContext(currentContext);
```

### Zipkin V2 Support

To configure the library to use Zipkin v2, set the `SpanReporter` like this:

```
.module(ServerTracingModule.class, config -> {
        config
            .serviceName("ratpack-demo")
            .spanReporterV2(reporter);
    })
```

...there `reporter` is some Zipkin v2 reporter.

E.g.

```
Reporter<Span> reporter =
    AsyncReporter.create(OkHttpSender
                         .create(String.format("http://%s:9411/api/v2/spans",
                                               okHttpHost)));
```

Note that v1 Reporter support is now *deprecated*.

## Ratpack-zipkin V1

### v1 Binaries

Using Gradle:

```
compile 'com.github.hyleung:ratpack-zipkin:1.2.1'
```

Using Maven:

```
<dependency>
  <groupId>com.github.hyleung</groupId>
  <artifactId>ratpack-zipkin</artifactId>
  <version>1.2.1</version>
</dependency>
```


### v1 Usage

#### SR/SS Spans

The minimal configuration:

```
RatpackServer.start(server -> server
    .serverConfig(config -> config.port(serverPort))
    .registry(Guice.registry(binding -> binding
        .module(ServerTracingModule.class, config -> config.serviceName("some-service-name")
        .bind(HelloWorldHandler.class)
    ))(
    .handlers(chain -> chain
        ...)
);
```

This should add a `HandlerDecorator` that adds server send (SS) and server receive (SS) tracing using the default settings.

There's a small demo app in [ratpack-zipkin-example](https://github.com/hyleung/ratpack-zipkin-example).

#### CS/CR Spans 
For tracing of Http client spans, use the `@Zipkin` annotation to inject a Zipkin instrumented implementation of the
Ratpack `HttpClient`.

```
@Inject
@Zipkin
HttpClient client
...
client.get(new URI("http://example.com", requestSpec -> ...))
    ... 
```

This annotation can be used for both field and constructor injection.

#### Local Spans

For local spans, use the normal Brave `LocalTracer`:

```
@Inject LocalTracer tracer

...
tracer.startNewSpan("My component", "an operation");
...
tracer.finishSpan();
```

Currently nested local spans don't work in v1, but are supported in v2.

#### Default Tags

For server requests, we record the following annotations:
- [TraceKeys.HTTP_METHOD](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_METHOD)
- [TraceKeys.HTTP_PATH](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_PATH)

For server responses, we record the following:
- [TraceKeys.HTTP_STATUS_CODE](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_STATUS_CODE) for all **non-2xx** responses

For client requests, we record the following:
- [TraceKeys.HTTP_METHOD](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_METHOD)
- [TraceKeys.HTTP_PATH](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_PATH)

For client responses, we record the following:
- [TraceKeys.HTTP_STATUS_CODE](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_STATUS_CODE) for all **non-2xx** responses


