# ratpack-zipkin

![Build status](https://travis-ci.org/hyleung/ratpack-zipkin.svg?branch=master) [![codecov.io](http://codecov.io/github/hyleung/ratpack-zipkin/coverage.svg?branch=master)](http://codecov.io/github/hyleung/ratpack-zipkin?branch=master)

[Zipkin](https://twitter.github.io/zipkin/index.html) support for [Ratpack](http://www.ratpack.io).

Uses [Brave](https://github.com/openzipkin/brave) for the underlying Zipkin support.

## Binaries

Using Gradle:

```
compile 'com.github.hyleung:ratpack-zipkin:1.0.0'
```

Using Maven:

```
<dependency>
  <groupId>com.github.hyleung</groupId>
  <artifactId>ratpack-zipkin</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Getting Started

### Zipkin

First you'll need to (obviously) get Zipkin up and running. The quickest way to do this is using Docker and `docker-compose`.

Clone [docker-zipkin](https://github.com/openzipkin/docker-zipkin)

Start the Zipkin stack by running;

```
docker-compose up
```

### Ratpack

#### SR/SS Spans

The mimimal configuration:

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

`ZipkinHttpClient` provides some HTTP client functionality - similar to Ratpack's own `HttpClient`. There are separate
methods for each of the supported HTTP methods (`GET`, `POST`, `PUT`, `PATCH`, `OPTIONS`, `HEAD`). Tracing of streamed
responses is *not* supported.

```
@Inject ZipkinHttpClient client
...
client.get(new URI("http://example.com", requestSpec -> ...))
    ... 
```

The underlying implementation is just a wrapper that delegates most of the work to a standard Ratpack `HttpClient` - 
just adds a little bit of instrumentation around the client request/response. Eventually, the hope is that this custom
Http client will go away, but that requires some extra hooks the normal Ratpack HTTP client.

#### Local Spans

For local spans, use the normal Brave `LocalTracer`:

```
@Inject LocalTracer tracer

...
tracer.startNewSpan("My component", "an operation");
...
tracer.finishSpan();
```

Currently nested local spans don't work - but hopefully soon!

