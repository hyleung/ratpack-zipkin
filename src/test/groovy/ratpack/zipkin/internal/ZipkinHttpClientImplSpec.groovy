package ratpack.zipkin.internal

import brave.Tracer
import brave.sampler.Sampler
import ratpack.exec.ExecResult
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.test.exec.ExecHarness
import ratpack.zipkin.support.TestReporter
import spock.lang.AutoCleanup
import spock.lang.Specification
import zipkin.Span
import zipkin.TraceKeys

class ZipkinHttpClientImplSpec extends Specification {

	@AutoCleanup
	ExecHarness harness = ExecHarness.harness()

	HttpClient httpClient = Mock(HttpClient)
	RequestSpec requestSpec = Mock(RequestSpec)
	ReceivedResponse receivedResponse = Mock(ReceivedResponse)
	HttpClient zipkinHttpClient
	TestReporter reporter

	URI uri = URI.create("http://localhost/test")
	Action<? super RequestSpec> action = Action.noop()

	def setup() {
		reporter = new TestReporter()

		Tracer tracer = Tracer.newBuilder()
				.currentTraceContext(new RatpackCurrentTraceContext())
				.reporter(reporter).sampler(Sampler.ALWAYS_SAMPLE)
				.localServiceName("embedded")
				.build()

		zipkinHttpClient = new ZipkinHttpClientImpl(httpClient, tracer, new DefaultSpanNameProvider())
	}

	def "get() should be traced"() {
		when:
		ExecResult<ReceivedResponse> rs = harness.yield { e ->
			zipkinHttpClient.get(uri, action)
		}

		then:
		rs.isSuccess()
		rs.value == receivedResponse
		reporter.spans.size() == 1
		Span span = reporter.spans.get(0)
		span.name == "get"
		span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_STATUS_CODE}.size() == 0
		span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_URL}.size() == 1
		span.annotations.findAll {it.value == "cs"}.size() == 1
		span.annotations.findAll {it.value == "cr"}.size() == 1

		1 * httpClient.request(_, _) >> { URI u, Action<? super RequestSpec> a ->
			assert u == uri
			a.execute(requestSpec)
			return Promise.value(receivedResponse)
		}
		1 * requestSpec.get() >> requestSpec
		1 * receivedResponse.getStatusCode() >> 200
		1 * requestSpec.getUri() >> uri
		0 * _
	}

}
