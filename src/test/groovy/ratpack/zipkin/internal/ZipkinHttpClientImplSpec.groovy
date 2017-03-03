package ratpack.zipkin.internal

import com.github.kristofa.brave.ClientRequestAdapter
import com.github.kristofa.brave.ClientRequestInterceptor
import com.github.kristofa.brave.ClientResponseAdapter
import com.github.kristofa.brave.ClientResponseInterceptor
import ratpack.exec.ExecResult
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.http.HttpMethod
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.http.client.StreamedResponse
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll

class ZipkinHttpClientImplSpec extends Specification {

    @AutoCleanup
    ExecHarness execHarness = ExecHarness.harness()

    HttpClient httpClient
    RequestSpec requestSpec
    ReceivedResponse receivedResponse
    ClientRequestInterceptor requestInterceptor
    ClientResponseInterceptor responseInterceptor
    ClientRequestAdapterFactory requestAdapterFactory
    ClientResponseAdapterFactory responseAdapterFactory
    ClientRequestAdapter clientRequestAdapter
    ClientResponseAdapter clientResponseAdapter
    HttpClient zipkinHttpClient

    URI uri = URI.create("http://localhost/test")
    Action<? super RequestSpec> action = Action.noop()

    def setup() {
        httpClient = Mock(HttpClient)
        requestSpec = Mock(RequestSpec)
        receivedResponse = Mock(ReceivedResponse)
        requestInterceptor = Mock(ClientRequestInterceptor)
        responseInterceptor = Mock(ClientResponseInterceptor)
        requestAdapterFactory = Mock(ClientRequestAdapterFactory)
        responseAdapterFactory = Mock(ClientResponseAdapterFactory)
        clientRequestAdapter = Mock(ClientRequestAdapter)
        clientResponseAdapter = Mock(ClientResponseAdapter)
        zipkinHttpClient = new ZipkinHttpClientImpl(
                httpClient,
                requestInterceptor,
                responseInterceptor,
                requestAdapterFactory,
                responseAdapterFactory
        )
    }

    @Unroll
    def "requestStream() with HttpMethod #method should be traced and delegated"() {
        StreamedResponse streamedResponse = Mock(StreamedResponse)
        Action<? super RequestSpec> setMethodAction = action.prepend { r -> r.method(method)}

        when:
        ExecResult<StreamedResponse> rs = execHarness.yield { e ->
            zipkinHttpClient.requestStream(uri, setMethodAction)
        }

        then:
        rs.isSuccess()
        rs.value == streamedResponse
        1 * httpClient.requestStream(_, _) >> { URI u, Action<? super RequestSpec> a ->
            assert u == uri
            a.execute(requestSpec)
            return Promise.value(streamedResponse)
        }
        2 * requestSpec.method(method) >> requestSpec
        1 * requestAdapterFactory.createAdaptor(requestSpec, method.name) >> clientRequestAdapter
        1 * requestInterceptor.handle(clientRequestAdapter)
        1 * responseAdapterFactory.createAdapter(streamedResponse) >> clientResponseAdapter
        1 * responseInterceptor.handle(clientResponseAdapter)
        0 * _

        where:
        method | _
        HttpMethod.GET | _
        HttpMethod.POST | _
        HttpMethod.PUT | _
        HttpMethod.DELETE | _
        HttpMethod.HEAD | _
        HttpMethod.PATCH  | _
        HttpMethod.OPTIONS | _
    }

    @Unroll
    "request() with HttpMethod #method should be traced and delegated"() {
        Action<? super RequestSpec> setMethodAction = action.prepend { r -> r.method(method)}

        when:
        ExecResult<ReceivedResponse> rs = execHarness.yield { e ->
            zipkinHttpClient.request(uri, setMethodAction)
        }

        then:
        rs.isSuccess()
        rs.value == receivedResponse
        1 * httpClient.request(_, _) >> { URI u, Action<? super RequestSpec> a ->
            assert u == uri
            a.execute(requestSpec)
            return Promise.value(receivedResponse)
        }
        2 * requestSpec.method(method) >> requestSpec
        1 * requestAdapterFactory.createAdaptor(requestSpec, method.name) >> clientRequestAdapter
        1 * requestInterceptor.handle(clientRequestAdapter)
        1 * responseAdapterFactory.createAdapter(receivedResponse) >> clientResponseAdapter
        1 * responseInterceptor.handle(clientResponseAdapter)
        0 * _

        where:
        method | _
        HttpMethod.GET | _
        HttpMethod.POST | _
        HttpMethod.PUT | _
        HttpMethod.DELETE | _
        HttpMethod.HEAD | _
        HttpMethod.PATCH  | _
        HttpMethod.OPTIONS | _
    }

    def "get() should be traced"() {
        when:
        ExecResult<ReceivedResponse> rs = execHarness.yield { e ->
            zipkinHttpClient.get(uri, action)
        }

        then:
        rs.isSuccess()
        rs.value == receivedResponse
        1 * httpClient.request(_, _) >> { URI u, Action<? super RequestSpec> a ->
            assert u == uri
            a.execute(requestSpec)
            return Promise.value(receivedResponse)
        }
        1 * requestSpec.get() >> requestSpec
        1 * requestSpec.method(HttpMethod.GET) >> requestSpec
        1 * requestAdapterFactory.createAdaptor(requestSpec, HttpMethod.GET.name) >> clientRequestAdapter
        1 * requestInterceptor.handle(clientRequestAdapter)
        1 * responseAdapterFactory.createAdapter(receivedResponse) >> clientResponseAdapter
        1 * responseInterceptor.handle(clientResponseAdapter)
        0 * _
    }

    def "post() should be traced"() {
        when:
        ExecResult<ReceivedResponse> rs = execHarness.yield { e ->
            zipkinHttpClient.post(uri, action)
        }

        then:
        rs.isSuccess()
        rs.value == receivedResponse
        1 * httpClient.request(_, _) >> { URI u, Action<? super RequestSpec> a ->
            assert u == uri
            a.execute(requestSpec)
            return Promise.value(receivedResponse)
        }
        1 * requestSpec.post() >> requestSpec
        1 * requestSpec.method(HttpMethod.POST) >> requestSpec
        1 * requestAdapterFactory.createAdaptor(requestSpec, HttpMethod.POST.name) >> clientRequestAdapter
        1 * requestInterceptor.handle(clientRequestAdapter)
        1 * responseAdapterFactory.createAdapter(receivedResponse) >> clientResponseAdapter
        1 * responseInterceptor.handle(clientResponseAdapter)
        0 * _
    }
}
