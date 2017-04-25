package ratpack.zipkin.internal

import brave.Tracing
import brave.sampler.Sampler
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.http.HttpMethod
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.http.client.StreamedResponse
import ratpack.test.exec.ExecHarness
import ratpack.zipkin.support.TestReporter
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll
import zipkin.Constants
import zipkin.Span
import zipkin.TraceKeys

import static org.assertj.core.api.Assertions.assertThat

class ZipkinHttpClientImplSpec extends Specification {
	
	@AutoCleanup
	ExecHarness harness = ExecHarness.harness()

	HttpClient httpClient = Mock(HttpClient)
	RequestSpec requestSpec = Mock(RequestSpec)
	ReceivedResponse receivedResponse = Mock(ReceivedResponse)
	ZipkinHttpClientImpl zipkinHttpClient
	TestReporter reporter

	URI uri = URI.create("http://localhost/test")
	Action<? super RequestSpec> action = Action.noop()

	def setup() {
		reporter = new TestReporter()

		Tracing tracing = Tracing.newBuilder()
				.currentTraceContext(new RatpackCurrentTraceContext())
				.reporter(reporter).sampler(Sampler.ALWAYS_SAMPLE)
				.localServiceName("embedded")
				.build()

		zipkinHttpClient = new ZipkinHttpClientImpl(httpClient, tracing, new DefaultSpanNameProvider())

		httpClient.request(_ as URI, _ as Action) >> { URI u, Action<? super RequestSpec> a ->
			a.execute(requestSpec)
			return Promise.value(this.receivedResponse)
		}
		requestSpec.get() >> requestSpec
	}
	@Unroll
	def "#method requests should be traced"(HttpMethod method) {
		given:
			receivedResponse.getStatusCode() >> 200
			requestSpec.getUri() >> uri
		when:
			harness.yield { e ->
				zipkinHttpClient.request(uri, {spec -> spec.method(method)})
			}
		then:
			reporter.spans.size() == 1
			Span span = reporter.spans.get(0)
			span.name == method.name.toLowerCase()
		and: "should contain CS annotation"
			assertThat(span.annotations.findAll {it.value == Constants.CLIENT_SEND}).isNotEmpty()
		and: "should contain CR annotation"
			assertThat(span.annotations.findAll {it.value == Constants.CLIENT_RECV}).isNotEmpty()
		where:
			method | _
			HttpMethod.GET | _
			HttpMethod.POST | _
			HttpMethod.PUT | _
			HttpMethod.PATCH | _
			HttpMethod.DELETE | _
			HttpMethod.HEAD | _
			HttpMethod.OPTIONS | _

	}
	def "Request returning 2xx include HTTP_URL annotation (but *not* status code)"(HttpResponseStatus status) {
		given:
			receivedResponse.getStatusCode() >> status.code()
			requestSpec.getUri() >> uri
		when:
			harness.yield { e ->
				zipkinHttpClient.get(uri, action)
			}
		then:
			Span span = reporter.spans.get(0)
		and: "should not contain http status code annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_STATUS_CODE}).isEmpty()
		and: "should contain http url annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_URL}).isNotEmpty()
		where:
			status | _
			HttpResponseStatus.OK | _
			HttpResponseStatus.ACCEPTED | _
			HttpResponseStatus.ACCEPTED | _
			HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION | _
			HttpResponseStatus.NO_CONTENT | _
			HttpResponseStatus.RESET_CONTENT | _
			HttpResponseStatus.PARTIAL_CONTENT | _
	}

	def "Request returning 4xx include HTTP_URL and HTTP_STATUS_CODE annotations"(HttpResponseStatus status) {
		given:
			receivedResponse.getStatusCode() >> status.code()
			requestSpec.getUri() >> uri

		when:
			harness.yield { e ->
				zipkinHttpClient.post(uri, action)
			}
		then:
			Span span = reporter.spans.get(0)
		and: "should contain http status code annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_STATUS_CODE}).isNotEmpty()
		and: "should contain http url annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_URL}).isNotEmpty()
		where:
			status | _
			HttpResponseStatus.BAD_REQUEST | _
			HttpResponseStatus.UNAUTHORIZED | _
			HttpResponseStatus.PAYMENT_REQUIRED | _
			HttpResponseStatus.FORBIDDEN | _
			HttpResponseStatus.NOT_FOUND | _
			HttpResponseStatus.METHOD_NOT_ALLOWED | _
			HttpResponseStatus.NOT_ACCEPTABLE | _
			HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED | _
			HttpResponseStatus.REQUEST_TIMEOUT | _
			HttpResponseStatus.CONFLICT | _
			HttpResponseStatus.GONE | _
	}

	def "Request returning 5xx include HTTP_URL and HTTP_STATUS_CODE annotations"(HttpResponseStatus status) {
		given:
			receivedResponse.getStatusCode() >> status.code()
			requestSpec.getUri() >> uri
		when:
			harness.yield { e ->
				zipkinHttpClient.get(uri, action)
			}
		then:
			Span span = reporter.spans.get(0)
		and: "should contain http status code annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_STATUS_CODE}).isNotEmpty()
		and: "should contain http url annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_URL}).isNotEmpty()
		and: "should contain error annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == Constants.ERROR}).isNotEmpty()
		where:
			status | _
			HttpResponseStatus.INTERNAL_SERVER_ERROR | _
			HttpResponseStatus.NOT_IMPLEMENTED | _
			HttpResponseStatus.BAD_GATEWAY | _
			HttpResponseStatus.SERVICE_UNAVAILABLE | _
			HttpResponseStatus.GATEWAY_TIMEOUT | _
			HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED | _
			HttpResponseStatus.VARIANT_ALSO_NEGOTIATES | _
	}

	def "Should trace streamed requests" () {
		given:
			StreamedResponse streamedResponse = Stub(StreamedResponse)
			streamedResponse.getStatusCode() >> 200
			requestSpec.getUri() >> uri
			httpClient.requestStream(_ as URI, _ as Action) >> { URI u, Action<? super RequestSpec> a ->
				a.execute(requestSpec)
				return Promise.value(streamedResponse)
			}
		when:
			StreamedResponse response = harness.yield { e ->
				zipkinHttpClient.requestStream(uri, action)
			}.value
		then:
			response.getStatusCode() == 200
			Span span = reporter.spans.get(0)
		and: "should not contain http status code annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_STATUS_CODE}).isEmpty()
		and: "should contain http url annotation"
			assertThat(span.binaryAnnotations.findAll {it.key == TraceKeys.HTTP_URL}).isNotEmpty()
	}
}
