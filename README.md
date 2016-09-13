# ratpack-zipkin

![Build status](https://travis-ci.org/hyleung/ratpack-zipkin.svg?branch=master) [![codecov.io](http://codecov.io/github/hyleung/ratpack-zipkin/coverage.svg?branch=master)](http://codecov.io/github/hyleung/ratpack-zipkin?branch=master)

[Zipkin](https://twitter.github.io/zipkin/index.html) support for [Ratpack](http://www.ratpack.io).

This repo is a work-in-progress for adding Zipkin support to Ratpack.

Uses [Brave](https://github.com/openzipkin/brave) for the underlying Zipkin support.

## Releases

Currently, only snapshot releases are available (at https://oss.sonatype.org/content/repositories/snapshots/).

Using Gradle:

```
compile 'com.github.hyleung:ratpack-zipkin:1.0.0-SNAPSHOT'
```

Using Maven:

```
<dependency>
  <groupId>com.github.hyleung</groupId>
  <artifactId>ratpack-zipkin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
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
