# ratpack-zipkin

![Build status](https://travis-ci.org/hyleung/ratpack-zipkin.svg?branch=master) [![codecov.io](http://codecov.io/github/hyleung/ratpack-zipkin/coverage.svg?branch=master)](http://codecov.io/github/hyleung/ratpack-zipkin?branch=master)

[Zipkin](https://twitter.github.io/zipkin/index.html) support for [Ratpack](http://www.ratpack.io).

Uses [Brave](https://github.com/openzipkin/brave) for the underlying Zipkin support.

## Binaries

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
RatPack `HttpClient`.

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

Currently nested local spans don't work - but hopefully soon!

#### Default KeyValueAnnotations

For server requests, we record the following annotations:
- [TraceKeys.HTTP_PATH](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_PATH)
- [TraceKeys.HTTP_METHOD](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_METHOD)

For server responses, we record the following:
- [TraceKeys.HTTP_STATUS_CODE](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_STATUS_CODE) for all **non-2xx** responses

For client requests, we record the following:
- [TraceKeys.HTTP_HOST](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_HOST)
- [TraceKeys.HTTP_PATH](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_PATH)

For client responses, we record the following:
- [TraceKeys.HTTP_STATUS_CODE](http://zipkin.io/zipkin/1.20.1/zipkin/zipkin/TraceKeys.html#HTTP_STATUS_CODE) for all **non-2xx** responses


