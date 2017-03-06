package ratpack.zipkin.internal

import io.netty.buffer.ByteBuf
import ratpack.func.Action
import ratpack.http.HttpMethod
import ratpack.http.client.RequestSpec
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.Charset


class MethodCapturingRequestSpecSpec extends Specification {
    MethodCapturingRequestSpec requestSpec;
    RequestSpec actualSpec;
    def setup() {
        actualSpec = Mock(RequestSpec)
        requestSpec = new MethodCapturingRequestSpec(actualSpec)
    }

    def "Should return underlying spec's URI"() {
        given:
            URI uri = new URI("http://foo.bar.com")
            actualSpec.getUri() >> uri
        expect:
            requestSpec.getUri() == uri
    }

    @Unroll
    def "Should capture request method for #httpMethod" (HttpMethod httpMethod) {
        given:
            Action<RequestSpec> configurer = {
                spec -> spec.method(httpMethod)
            }
        when:
            configurer.execute(requestSpec)
        then:
            requestSpec.capturedMethod == httpMethod
        where:
            httpMethod | _
            HttpMethod.GET | _
            HttpMethod.POST | _
            HttpMethod.PUT | _
            HttpMethod.DELETE | _
            HttpMethod.HEAD | _
            HttpMethod.PATCH | _
            HttpMethod.OPTIONS | _
    }

    def "Should return non-null headers"() {
        expect:
            requestSpec.getHeaders() != null
    }

    def "Should return non-null body that supports the expected operations"() {
        expect:
            requestSpec.getBody() != null
        and:
            //lame, but just checking to make sure that MethodCapturingRequestSpec
            //can be used in the same way as any other RequestSpec
            requestSpec
                    .getBody()
                    .buffer(Stub(ByteBuf))
                    .bytes(new byte[0])
                    .text("foo")
                    .text("foo", Charset.defaultCharset())
                    .stream{ os -> }
    }
}
