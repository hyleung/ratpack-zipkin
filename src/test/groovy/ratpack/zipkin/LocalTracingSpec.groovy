package ratpack.zipkin

import brave.Span
import brave.Tracer
import brave.Tracer.SpanInScope
import brave.Tracing
import brave.sampler.Sampler
import ratpack.exec.Execution
import ratpack.test.exec.ExecHarness
import ratpack.zipkin.internal.RatpackCurrentTraceContext
import ratpack.zipkin.support.TestReporter
import spock.lang.AutoCleanup
import spock.lang.Specification

class LocalTracingSpec extends Specification {

	@AutoCleanup
	ExecHarness harness = ExecHarness.harness()

	TestReporter reporter = new TestReporter()
	Tracing tracing

	Tracing.Builder tracingBuilder(Execution execution) {
		return Tracing.newBuilder().reporter(reporter)
		.currentTraceContext(new RatpackCurrentTraceContext({ -> execution }))
		.sampler(Sampler.ALWAYS_SAMPLE)
	}

	def cleanup() {
		reporter.reset()
	}

	def "Should propagate trace context to nested spans"() {

		when:
		harness.yield { e ->
			tracing = tracingBuilder(e).build()
			Tracer tracer = tracing.tracer()

			Span parent = tracer.newTrace().name("parent")
			SpanInScope ws1 = tracer.withSpanInScope(parent)
			try {
				Span child1 = tracer.newChild(parent.context()).name("child1")
				SpanInScope ws2 = tracer.withSpanInScope(child1)
				try {
					Span child2 = tracer.newChild(child1.context()).name("child2")
					SpanInScope ws3 = tracer.withSpanInScope(child2)
					try {

					} finally {
						ws3.close()
						child2.finish()
					}
				} finally {
					ws2.close()
					child1.finish()
				}
			} finally {
				ws1.close()
				parent.finish()
			}
		}

		then:
		List<zipkin.Span> spans = reporter.spans
		spans.size() == 3
		zipkin.Span parent = spans.find {it.name == "parent"}
		zipkin.Span child1 = spans.find {it.name == "child1"}
		zipkin.Span child2 = spans.find {it.name == "child2"}

		assert parent
		assert child1
		assert child2

		child1.parentId == parent.id
		child2.parentId == child1.id
		child1.traceId == parent.traceId
		child2.traceId == parent.traceId
	}

}
