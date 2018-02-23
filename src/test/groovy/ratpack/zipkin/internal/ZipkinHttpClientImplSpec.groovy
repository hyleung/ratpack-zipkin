package ratpack.zipkin.internal

import brave.Tracing
import brave.http.HttpTracing
import brave.sampler.Sampler
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.handler.codec.http.HttpResponseStatus
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import ratpack.exec.Execution
import ratpack.func.Action
import ratpack.http.HttpMethod
import ratpack.http.client.HttpClient
import ratpack.http.client.RequestSpec
import ratpack.http.client.StreamedResponse
import ratpack.server.ServerConfig
import ratpack.test.exec.ExecHarness
import ratpack.zipkin.support.TestReporter
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll
import zipkin2.Span
import zipkin.TraceKeys

import static org.assertj.core.api.Assertions.assertThat

class ZipkinHttpClientImplSpec extends Specification {

	MockWebServer webServer
	URI uri
	@AutoCleanup
	ExecHarness harness = ExecHarness.harness()

	ZipkinHttpClientImpl zipkinHttpClient
	TestReporter reporter

	Action<? super RequestSpec> action = Action.noop()

	def setup() {
		webServer = new MockWebServer()
		webServer.start()
		uri = webServer.url("/").url().toURI()
		reporter = new TestReporter()
	}

	def cleanup() {
		webServer.shutdown()
	}

	void harnessSetup(Execution e) {
		HttpTracing httpTracing = HttpTracing.create(Tracing.newBuilder()
				.currentTraceContext(new RatpackCurrentTraceContext({ -> e}))
				.spanReporter(reporter).sampler(Sampler.ALWAYS_SAMPLE)
				.localServiceName("embedded")
				.build())

		zipkinHttpClient = new ZipkinHttpClientImpl(HttpClient.of { s ->
			s.poolSize(0)
			 .byteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
			  .maxContentLength(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH)}, httpTracing)
	}

	@Unroll
	def "#method requests should be traced"(HttpMethod method) {
		given:
			webServer.enqueue(new MockResponse().setResponseCode(200))
		when:
			harness.yield { e ->
				harnessSetup(e)
				zipkinHttpClient.request(uri, {spec -> spec.method(method)})
			}
		then:
			reporter.spans.size() == 1
			Span span = reporter.spans.get(0)
			span.name() == method.name.toLowerCase()
		and: "should contain client span"
			assertThat(span.kind() == Span.Kind.CLIENT)
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

	def "Request should not duplicate headers sent"() {
		given:
			webServer.enqueue(new MockResponse().setResponseCode(200))

		when:
			harness.yield { e ->
				harnessSetup(e)
				zipkinHttpClient.request(uri, {spec ->
					spec.method(HttpMethod.GET).headers.add("X-TEST", "test")
				})
			}

		then:
			reporter.spans.size() == 1
			Span span = reporter.spans.get(0)
			span.name == "get"

		then:
			RecordedRequest request = webServer.takeRequest()
			request.headers.values("X-TEST").size() == 1
	}

	def "Request returning 2xx includes HTTP_METHOD and HTTP_PATH tags (but *not* status code)"(HttpResponseStatus status) {
		given:
			webServer.enqueue(new MockResponse().setResponseCode(status.code()))
		when:
			harness.yield { e ->
				harnessSetup(e)
				zipkinHttpClient.get(uri, action)
			}.value
		then:
			Span span = reporter.spans.get(0)
		and: "should contain method and path tag but not status code tag"
			assertThat(span.tags()).containsOnlyKeys(TraceKeys.HTTP_METHOD, TraceKeys.HTTP_PATH)
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

	def "Request returning 4xx includes HTTP_METHOD, HTTP_PATH and HTTP_STATUS_CODE tags"(HttpResponseStatus status) {
		given:
			webServer.enqueue(new MockResponse().setResponseCode(status.code()))
		when:
			harness.yield { e ->
				harnessSetup(e)
				zipkinHttpClient.post(uri, action)
			}
		then:
			Span span = reporter.spans.get(0)
		and: "should contain http status code, path and error tags"
			assertThat(span.tags()).containsOnlyKeys(TraceKeys.HTTP_METHOD, TraceKeys.HTTP_PATH, TraceKeys.HTTP_STATUS_CODE, "error")
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

	def "Request returning 5xx includes HTTP_METHOD, HTTP_PATH and HTTP_STATUS_CODE tags"(HttpResponseStatus status) {
		given:
			webServer.enqueue(new MockResponse().setResponseCode(status.code()))
		when:
			harness.yield { e ->
				harnessSetup(e)
				zipkinHttpClient.get(uri, action)
			}
		then:
			Span span = reporter.spans.get(0)
		and: "should contain status, path and error tags"
			assertThat(span.tags()).containsOnlyKeys(TraceKeys.HTTP_METHOD, TraceKeys.HTTP_PATH, TraceKeys.HTTP_STATUS_CODE, "error")
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
			webServer.enqueue(new MockResponse().setResponseCode(200))
		when:
			StreamedResponse response = harness.yield { e ->
				harnessSetup(e)
				zipkinHttpClient.requestStream(uri, action.append({ RequestSpec s -> s.get()}))
			}.value
		then:
			response.getStatusCode() == 200
			Span span = reporter.spans.get(0)
		and: "should contain method and path tags, but not status code tag"
			assertThat(span.tags()).containsOnlyKeys(TraceKeys.HTTP_METHOD, TraceKeys.HTTP_PATH)
	}
}
