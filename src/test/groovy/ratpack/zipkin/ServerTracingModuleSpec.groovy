package ratpack.zipkin

import brave.SpanCustomizer
import brave.http.HttpSampler
import brave.propagation.B3Propagation
import brave.sampler.Sampler
import io.netty.handler.codec.http.HttpResponseStatus
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.http.client.HttpClient
import ratpack.path.PathBinding
import ratpack.zipkin.support.B3PropagationHeaders
import ratpack.zipkin.support.TestReporter
import spock.lang.Specification
import spock.lang.Unroll
import zipkin2.Span
import zipkin2.reporter.Reporter

import static org.assertj.core.api.Assertions.assertThat
import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class ServerTracingModuleSpec extends Specification {

	TestReporter reporter

	def setup() {
		reporter = new TestReporter()
	}

	def 'Should initialize with default config'() {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule)
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.render("foo")
					}

				}
			}
		expect:
			app.test {
				assert getText() == "foo"
			}
	}

	def 'Should initialize with all the configs'() {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, {config ->
						config
							.serviceName("embedded")
							.sampler(Sampler.create(1f))
							.clientSampler(HttpSampler.TRACE_ID)
							.serverSampler(HttpSampler.TRACE_ID)
							.spanReporterV2(Reporter.NOOP)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.render("foo")
					}
				}
			}
		expect:
			app.test {
				assert getText() == "foo"
			}
	}

	def 'Should collect server spans with Reporter'() {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.render("foo")
					}
				}
			}
		when:
			app.test { assert getText() == "foo" }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
		and: "should be server span"
			span.kind() == Span.Kind.SERVER
	}

	@Unroll
	def "Should collect spans with HTTP #method"(HttpMethod method) {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.render("foo")
					}
				}
			}
		when:
			app.test {
				request { spec ->
					spec.method(method)
				}
			}
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.name() == method.name.toLowerCase()
		where:
			method             | _
			HttpMethod.GET     | _
			HttpMethod.POST    | _
			HttpMethod.PUT     | _
			HttpMethod.PATCH   | _
			HttpMethod.DELETE  | _
			HttpMethod.HEAD    | _
			HttpMethod.OPTIONS | _
	}

	def 'Should join trace if B3 propagation headers present'() {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.render("foo")
					}
				}
			}
		when:
			app.test {
				request { spec ->
					spec.get()
						.headers { headers ->
							headers
									.add(B3PropagationHeaders.TRACE_ID.value, "0000000000000001")
									.add(B3PropagationHeaders.PARENT_ID.value, "0000000000000001")
									.add(B3PropagationHeaders.SPAN_ID.value, "0000000000000001")
									.add(B3PropagationHeaders.SAMPLED.value, "1")
					}
				}
			}
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.name() == "get"
			span.traceId() == "0000000000000001"
			span.parentId() == "0000000000000001"
			span.id() == "0000000000000001"
			span.kind() == Span.Kind.SERVER
	}

	def 'Should report span with http status code tag for 1xx (#status) responses'(HttpResponseStatus status) {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.response.status(status.code()).send()
					}
				}
			}
		when:
			app.test { assert getText() == "" }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.tags().find { it -> it.key == "http.status_code" } != null
		where:
			status                                 | _
			HttpResponseStatus.CONTINUE            | _
			HttpResponseStatus.SWITCHING_PROTOCOLS | _
	}

	def 'Should report span with http status code tag for 3xx (#status) responses'(HttpResponseStatus status) {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.response.status(status.code()).send()
					}
				}
			}
		when:
			app.test { assert getText() == "" }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.tags().find { it -> it.key == "http.status_code" } != null
			span.tags().find { it -> it.key == "error" } == null
		where:
			status                                | _
			HttpResponseStatus.MOVED_PERMANENTLY  | _
			HttpResponseStatus.FOUND              | _
			HttpResponseStatus.MOVED_PERMANENTLY  | _
			HttpResponseStatus.SEE_OTHER          | _
			HttpResponseStatus.NOT_MODIFIED       | _
			HttpResponseStatus.USE_PROXY          | _
			HttpResponseStatus.TEMPORARY_REDIRECT | _
	}


	def 'Should report span with http status code tag for 4xx (#status) responses'(HttpResponseStatus status) {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.response.status(status.code()).send()
					}
				}
			}
		when:
			app.test { assert getText() == "" }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.tags().find { it -> it.key == "http.status_code" } != null
			span.tags().find { it -> it.key == "error" } != null
		where:
			status                                           | _
			HttpResponseStatus.BAD_REQUEST                   | _
			HttpResponseStatus.UNAUTHORIZED                  | _
			HttpResponseStatus.PAYMENT_REQUIRED              | _
			HttpResponseStatus.FORBIDDEN                     | _
			HttpResponseStatus.NOT_FOUND                     | _
			HttpResponseStatus.METHOD_NOT_ALLOWED            | _
			HttpResponseStatus.NOT_ACCEPTABLE                | _
			HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED | _
			HttpResponseStatus.REQUEST_TIMEOUT               | _
			HttpResponseStatus.CONFLICT                      | _
			HttpResponseStatus.GONE                          | _
	}

	def 'Should report span with http status code tag for 5xx (#status) responses'(HttpResponseStatus status) {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.response.status(status.code()).send()
					}
				}
			}
		when:
			app.test { assert getText() == "" }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.tags().find { it -> it.key == "http.status_code" } != null
			span.tags().find { it -> it.key == "error" } != null
		where:
			status                                        | _
			HttpResponseStatus.INTERNAL_SERVER_ERROR      | _
			HttpResponseStatus.NOT_IMPLEMENTED            | _
			HttpResponseStatus.BAD_GATEWAY                | _
			HttpResponseStatus.SERVICE_UNAVAILABLE        | _
			HttpResponseStatus.GATEWAY_TIMEOUT            | _
			HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED | _
			HttpResponseStatus.VARIANT_ALSO_NEGOTIATES    | _
	}

	def 'Should not collect spans with 0 pct. sampling'() {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.NEVER_SAMPLE)
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.render("foo")
					}
				}
			}
		when:
			app.test { getText() == "" }

		then:
			reporter.getSpans().size() == 0
	}

	def 'Should collect spans with B3 header override sampling'() {
		given:
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.NEVER_SAMPLE)
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all {
						ctx -> ctx.render("foo")
					}
				}
			}
		when:
			app.test {
				request { spec -> spec
						.get()
						.headers { headers ->
							headers.add(B3PropagationHeaders.SAMPLED.value, "1")
						}
				}
			}
		then:
			assertThat(reporter.getSpans()).isNotEmpty()
	}

	def 'Should nest client span and server span should have the same trace id'() {
		given:
			def webServer = new MockWebServer()
			webServer.start()
			webServer.enqueue(new MockResponse().setResponseCode(200))
			def url = webServer.url("/")
		and: 'a handler that uses http client to call another service'
			def app = ratpack {
				bindings {
					module(ServerTracingModule, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.ALWAYS_SAMPLE)
								.spanReporterV2(reporter)
					})
				}
				handlers {
					chain -> chain.all { ctx ->
						def client = ctx.get(HttpClient.class)
						client.get(url.url().toURI())
								.then{ resp -> ctx.render("Got response from client: " + resp.getStatusCode()) }
					}
				}
			}

		when: 'the handler is invoked'
			app.test {
				request { spec ->
					spec.get()
				}
			}
			def spans = reporter.getSpans()
		then: 'the correct number of spans is reported (one server span, one client span)'
			assertThat(spans).isNotEmpty()
			assertThat(spans).hasSize(2)
		and: 'contains both server and client span kinds'
			assertThat(spans*.kind()).contains(Span.Kind.SERVER, Span.Kind.CLIENT)
		cleanup:
			webServer.shutdown()

	}

	def 'Should customize current span'() {
		given:
      def app = ratpack {
        bindings {
          module(ServerTracingModule, { config ->
            config
                .serviceName("embedded")
                .sampler(Sampler.create(1f))
                .spanReporterV2(reporter)
          })
        }
        handlers {
          chain -> chain.all { ctx ->
            ctx.get(SpanCustomizer)
                .tag("key1", "one")
                .tag("key2", "two")

            ctx.render("foo")
          }
        }
      }
		when:
      app.test {
        request { spec ->
          spec.method(HttpMethod.GET)
        }
      }
		then:
      reporter.getSpans().size() == 1
      Span span = reporter.getSpans().get(0)
      span.tags().get("key1") == "one"
      span.tags().get("key2") == "two"
	}
	def 'Should allow span name customization'() {
		given:
      def app = ratpack {
        bindings {
          module(ServerTracingModule, { config ->
            config
              .serviceName("embedded")
              .sampler(Sampler.ALWAYS_SAMPLE)
              .spanReporterV2(reporter)
              .spanNameProvider(new SpanNameProvider() {
              @Override
              String spanName(
                final ServerRequest requestContext,
                final Optional<PathBinding> pathBindingOpt) {
                return pathBindingOpt
                  .map { pathBinding -> pathBinding.getDescription() }
                  .orElse(requestContext.path)
              }
            })
          })
        }
        handlers {
          chain ->
            chain.get("say/:message", new Handler() {
              @Override
              void handle(final Context ctx) throws Exception {
                ctx.response.send("yo!")
              }
            })
        }
      }
    when:
      app.test {
        assert getText("say/hello") == "yo!"
      }
    then:
      assertThat(reporter.getSpans()).isNotEmpty()
      def span = reporter.getSpans().first()
      assertThat(span.name()).isEqualTo("get /say/:message")
  }

	def 'Should allow configuration of PropagationFactory'() {
    given:
      def app = ratpack {
        bindings {
          module(ServerTracingModule, { config ->
            config
              .serviceName("embedded")
              .sampler(Sampler.ALWAYS_SAMPLE)
              .spanReporterV2(reporter)
              .propagationFactory(B3Propagation.FACTORY)
          })
        }
        handlers {
          chain ->
            chain.get("say/:message", new Handler() {
              @Override
              void handle(final Context ctx) throws Exception {
                ctx.response.send("yo!")
              }
            })
        }
      }
		when:
      app.test {
        assert getText("say/hello") == "yo!"
      }
		then:
      assertThat(reporter.getSpans()).isNotEmpty()
	}
}