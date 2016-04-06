package ratpack.zipkin.internal

import com.github.kristofa.brave.ServerRequestAdapter
import com.github.kristofa.brave.http.BraveHttpHeaders
import com.github.kristofa.brave.http.SpanNameProvider
import ratpack.http.Headers
import ratpack.http.Request
import spock.lang.Specification

import static org.junit.Assert.assertEquals

/**
 * Test suite for {@link RatpackServerRequestAdapter}.
 */
class RatpackServerRequestAdapterSpec extends Specification {
    def Request request = Stub(Request)
    def SpanNameProvider spanNameProvider = Stub(SpanNameProvider)
    ServerRequestAdapter adapter
    def setup() {
         adapter = new RatpackServerRequestAdapter(spanNameProvider, request)
    }

    def 'getTraceData should set sampled flag'(String headerValue, boolean expected) {
        setup:
            request.getHeaders() >>
                    Stub(Headers) {
                        get(BraveHttpHeaders.Sampled.getName()) >> headerValue
                    }
        when:
            def traceData = adapter.getTraceData()
        then:
            assertEquals(expected, traceData.getSample())
        where:
            headerValue | expected
            "0"         | false
            "false"     | false
            "1"         | true
            "true"      | true
    }

}
