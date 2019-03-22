package ratpack.zipkin

import brave.Span
import brave.Tracer
import brave.Tracing
import brave.sampler.Sampler
import ratpack.exec.Blocking
import ratpack.exec.ExecResult
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import ratpack.zipkin.internal.RatpackCurrentTraceContext
import ratpack.zipkin.support.TestReporter
import spock.lang.AutoCleanup
import spock.lang.Specification

class TracedParallelBatchSpec extends Specification {

	@AutoCleanup
	ExecHarness harness = ExecHarness.harness(3)

  TestReporter reporter = new TestReporter()
	Tracing tracing

	Tracing.Builder tracingBuilder(Execution execution) {
		return Tracing.newBuilder().spanReporter(reporter)
		.currentTraceContext(new RatpackCurrentTraceContext({ -> execution }))
		.sampler(Sampler.ALWAYS_SAMPLE)
	}

	def cleanup() {
		reporter.reset()
	}

	def "Should propagate trace context to parallel batch jobs"() {

		when:
		List<? extends ExecResult<String>> results = harness.yield { execution ->
			tracing = tracingBuilder(execution).build()
			Tracer tracer = tracing.tracer()

			Span parent = tracer.newTrace().name("parent").start()
			Tracer.SpanInScope ws = tracer.withSpanInScope(parent)

			Promise<String> promise1 = Blocking.get {
				Span span = tracer.newChild(parent.context()).name("promise1").start()
				Tracer.SpanInScope pws = tracer.withSpanInScope(span)
				try {
					sleep(10)
					return "promise1"
				} finally {
					ws.close()
					span.finish()
				}
			}

			Promise<String> promise2 = Blocking.get {
				sleep(5)
				Span span = tracer.newChild(parent.context()).name("promise2").start()
				Tracer.SpanInScope pws = tracer.withSpanInScope(span)
				try {
					sleep(20)
					return "promise2"
				} finally {
					ws.close()
					span.finish()
				}

			}

			try {

				TracedParallelBatch.of(promise1, promise2)
						.withContext(parent.context())
						.yieldAll()

			} finally {
				ws.close()
				parent.finish()
			}

		}.value

		then: 'results contain expected values'
		results.size() == 2
		results.find {it.value == "promise1"}
		results.find {it.value == "promise2"}

		and: 'spans contain expected values'
		List<zipkin2.Span> spans = reporter.spans
		spans.size() == 3
		zipkin2.Span parent = spans.find {it.name() == "parent"}
		zipkin2.Span promise1 = spans.find {it.name() == "promise1"}
		zipkin2.Span promise2 = spans.find {it.name() == "promise2"}

		promise1.parentId() == parent.id()
		promise1.traceId() == parent.traceId()
		promise2.parentId() == parent.id()
		promise2.traceId() == parent.traceId()

	}

}
