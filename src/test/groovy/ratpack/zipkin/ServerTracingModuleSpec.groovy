package ratpack.zipkin

import brave.sampler.Sampler
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.guice.Guice
import ratpack.zipkin.support.TestReporter
import spock.lang.Specification
import zipkin.Span
import zipkin.reporter.Reporter

class ServerTracingModuleSpec extends Specification {

	def 'Should initialize with default config'() {
		given:
		def app = GroovyEmbeddedApp.of { server ->
			server.registry(Guice.registry { binding ->
				binding.module(ServerTracingModule.class)
			}).handlers {
				chain -> chain.all {
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
				binding.module(ServerTracingModule.class, { config -> config
						.serviceName("embedded")
						.sampler(Sampler.create(1f))
						.spanReporter(Reporter.NOOP)
				})
			}).handlers {
				chain -> chain.all {
					ctx -> ctx.render("foo")
				}
			}
		}
		expect:
		app.test { t -> t.get() }
	}

	def 'Should collect spans with Reporter'() {
		given:
		def reporter = new TestReporter()
		def app = GroovyEmbeddedApp.of { server ->
			server.registry(Guice.registry { binding ->
				binding.module(ServerTracingModule.class, { config -> config
						.serviceName("embedded")
						.sampler(Sampler.create(1f))
						.spanReporter(reporter)
				})
			}).handlers {
				chain -> chain.all {
					ctx -> ctx.render("foo")
				}
			}
		}

		when:
		app.test { t -> t.get()}

		then:
		reporter.getSpans().size() == 1
		Span span = reporter.getSpans().get(0)
		span.name == "get"
	}

	def 'Should not collect spans with 0 pct. sampling'() {
		given:
		def reporter = new TestReporter()
		def app = GroovyEmbeddedApp.of { server ->
			server.registry(Guice.registry { binding ->
				binding.module(ServerTracingModule.class, { config -> config
						.serviceName("embedded")
						.sampler(Sampler.NEVER_SAMPLE)
						.spanReporter(reporter)
				})
			}).handlers {
				chain -> chain.all {
					ctx -> ctx.render("foo")
				}
			}
		}

		when:
		app.test { t -> t.get()}

		then:
		reporter.getSpans().size() == 0
	}

}
