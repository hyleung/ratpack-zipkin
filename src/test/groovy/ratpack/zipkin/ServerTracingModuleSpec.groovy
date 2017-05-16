package ratpack.zipkin

import brave.http.HttpSampler
import ratpack.zipkin.support.B3PropagationHeaders
import spock.lang.Unroll

import static org.assertj.core.api.Assertions.*

import brave.sampler.Sampler
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.guice.Guice
import ratpack.http.HttpMethod
import ratpack.zipkin.support.TestReporter
import spock.lang.Specification
import zipkin.Constants
import zipkin.Span
import zipkin.TraceKeys
import zipkin.reporter.Reporter

class ServerTracingModuleSpec extends Specification {

	TestReporter reporter

	def setup() {
		reporter = new TestReporter()
	}

	def 'Should initialize with default config'() {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class)
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.render("foo")
						}
				}
			}
		expect:
			app.test { t -> t.get() }
	}

	def 'Should initialize with all the configs'() {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.clientSampler(HttpSampler.TRACE_ID)
								.serverSampler(HttpSampler.TRACE_ID)
								.spanReporter(Reporter.NOOP)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.render("foo")
						}
				}
			}
		expect:
			app.test { t -> t.get() }
	}


	def 'Should collect SR/SS spans with Reporter'() {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.render("foo")
						}
				}
			}
		when:
			app.test { t -> t.get() }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
		and: "should contain SS annotation"
			span.annotations.findAll { it.value == Constants.SERVER_SEND }.size() == 1
		and: "should contain SR annotation"
			span.annotations.findAll { it.value == Constants.SERVER_RECV }.size() == 1
	}

	@Unroll
	def "Should collect spans with HTTP #method"(HttpMethod method) {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.render("foo")
						}
				}
			}
		when:
			app.test { t ->
				t.request { spec ->
					spec.method(method)
				}
			}
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.name == method.name.toLowerCase()
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
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.render("foo")
						}
				}
			}
		when:
			app.test { t ->
				t.request { spec ->
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
			span.name == "get"
			span.traceId == 1L
			span.parentId == 1L
			span.id == 1L
			span.annotations.findAll { it.value == "ss" }.size() == 1
			span.annotations.findAll { it.value == "sr" }.size() == 1
	}


	def 'Should report span with http status code binary annotation for 1xx (#status) responses'(HttpResponseStatus status) {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.response.status(status.code()).send()
						}
				}
			}
		when:
			app.test { t -> t.get() }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.binaryAnnotations.find { it -> it.key == TraceKeys.HTTP_STATUS_CODE } != null
		where:
			status                                 | _
			HttpResponseStatus.CONTINUE            | _
			HttpResponseStatus.SWITCHING_PROTOCOLS | _
	}

	def 'Should report span with http status code binary annotation for 3xx (#status) responses'(HttpResponseStatus status) {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.response.status(status.code()).send()
						}
				}
			}
		when:
			app.test { t -> t.get() }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.binaryAnnotations.find { it -> it.key == TraceKeys.HTTP_STATUS_CODE } != null
			span.binaryAnnotations.find { it -> it.key == Constants.ERROR } == null
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


	def 'Should report span with http status code binary annotation for 4xx (#status) responses'(HttpResponseStatus status) {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.response.status(status.code()).send()
						}
				}
			}
		when:
			app.test { t -> t.get() }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.binaryAnnotations.find { it -> it.key == TraceKeys.HTTP_STATUS_CODE } != null
			span.binaryAnnotations.find { it -> it.key == Constants.ERROR } != null
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

	def 'Should report span with http status code binary annotation for 5xx (#status) responses'(HttpResponseStatus status) {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.create(1f))
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.response.status(status.code()).send()
						}
				}
			}
		when:
			app.test { t -> t.get() }
		then:
			reporter.getSpans().size() == 1
			Span span = reporter.getSpans().get(0)
			span.binaryAnnotations.find { it -> it.key == TraceKeys.HTTP_STATUS_CODE } != null
			span.binaryAnnotations.find { it -> it.key == Constants.ERROR } != null
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
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.NEVER_SAMPLE)
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.render("foo")
						}
				}
			}
		when:
			app.test { t -> t.get() }

		then:
			reporter.getSpans().size() == 0
	}

	def 'Should collect spans with B3 header override sampling'() {
		given:
			def app = GroovyEmbeddedApp.of { server ->
				server.registry(Guice.registry { binding ->
					binding.module(ServerTracingModule.class, { config ->
						config
								.serviceName("embedded")
								.sampler(Sampler.NEVER_SAMPLE)
								.spanReporter(reporter)
					})
				}).handlers {
					chain ->
						chain.all {
							ctx -> ctx.render("foo")
						}
				}
			}
		when:
			app.test { t ->
				t.request { spec ->
					spec
							.get()
							.headers { headers ->
						headers.add(B3PropagationHeaders.SAMPLED.value, "1")
					}
				}
			}
		then:
			assertThat(reporter.getSpans()).isNotEmpty()
	}
}