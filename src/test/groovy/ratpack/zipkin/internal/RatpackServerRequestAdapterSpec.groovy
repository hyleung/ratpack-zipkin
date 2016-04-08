package ratpack.zipkin.internal

import com.github.kristofa.brave.IdConversion
import com.github.kristofa.brave.ServerRequestAdapter
import com.github.kristofa.brave.http.BraveHttpHeaders
import com.github.kristofa.brave.http.HttpServerRequest
import com.github.kristofa.brave.http.SpanNameProvider
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assert.assertEquals

/**
 * Test suite for {@link RatpackServerRequestAdapter}.
 */
class RatpackServerRequestAdapterSpec extends Specification {
    def HttpServerRequest request = Stub(HttpServerRequest)
    def SpanNameProvider spanNameProvider = Stub(SpanNameProvider)
    ServerRequestAdapter adapter
    def setup() {
         adapter = new RatpackServerRequestAdapter(spanNameProvider, request)
    }

    @Unroll
    def 'getTraceData should set sampled flag'(String headerValue, boolean expected) {
        setup:
            request.getHttpHeaderValue(BraveHttpHeaders.Sampled.getName()) >> headerValue
            def parentId = "7b"
            def traceId = "1c8"
            def spanId = "315"
            request.getHttpHeaderValue(BraveHttpHeaders.ParentSpanId.getName()) >> parentId
            request.getHttpHeaderValue(BraveHttpHeaders.TraceId.getName()) >> traceId
            request.getHttpHeaderValue(BraveHttpHeaders.SpanId.getName()) >> spanId
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

    def 'getTraceData should build SpanId from (correctly encoded) headers if sampled'() {
        setup:
            def parentId = "7b"
            def traceId = "1c8"
            def spanId = "315"
            request.getHttpHeaderValue(BraveHttpHeaders.Sampled.getName()) >> "1"
            request.getHttpHeaderValue(BraveHttpHeaders.ParentSpanId.getName()) >> parentId
            request.getHttpHeaderValue(BraveHttpHeaders.TraceId.getName()) >> traceId
            request.getHttpHeaderValue(BraveHttpHeaders.SpanId.getName()) >> spanId
        when:
            def traceData = adapter.getTraceData()
            def span = traceData.getSpanId()
        then:
            span.getParentSpanId() == IdConversion.convertToLong(parentId)
            span.getTraceId() == IdConversion.convertToLong(traceId)
            span.getSpanId() == IdConversion.convertToLong(spanId)
    }

    def 'getTraceData should build SpanId if parentId is absent'() {
        setup:
            def traceId = "1c8"
            def spanId = "315"
            request.getHttpHeaderValue(BraveHttpHeaders.Sampled.getName()) >> "1"
            request.getHttpHeaderValue(BraveHttpHeaders.ParentSpanId.getName()) >> null
            request.getHttpHeaderValue(BraveHttpHeaders.TraceId.getName()) >> traceId
            request.getHttpHeaderValue(BraveHttpHeaders.SpanId.getName()) >> spanId
        when:
            def traceData = adapter.getTraceData()
            def span = traceData.getSpanId()
        then:
            span.getParentSpanId() == null
            span.getTraceId() == IdConversion.convertToLong(traceId)
            span.getSpanId() == IdConversion.convertToLong(spanId)
    }

    def 'getTraceData should NOT build SpanId if traceId header not present'() {
        setup:
            def parentId = "7b"
            def spanId = "315"
            request.getHttpHeaderValue(BraveHttpHeaders.Sampled.getName()) >> "1"
            request.getHttpHeaderValue(BraveHttpHeaders.ParentSpanId.getName()) >> parentId
            request.getHttpHeaderValue(BraveHttpHeaders.TraceId.getName()) >> null
            request.getHttpHeaderValue(BraveHttpHeaders.SpanId.getName()) >> spanId
        when:
            def traceData = adapter.getTraceData()
        then:
            traceData.getSpanId() == null
    }

    def 'getTraceData should NOT build SpanId if spanId header not present'() {
        setup:
            def parentId = "7b"
            def tracId = "315"
            request.getHttpHeaderValue(BraveHttpHeaders.Sampled.getName()) >> "1"
            request.getHttpHeaderValue(BraveHttpHeaders.ParentSpanId.getName()) >> parentId
            request.getHttpHeaderValue(BraveHttpHeaders.TraceId.getName()) >> tracId
            request.getHttpHeaderValue(BraveHttpHeaders.SpanId.getName()) >> null
        when:
            def traceData = adapter.getTraceData()
        then:
            traceData.getSpanId() == null
    }

    def 'getTraceData should NOT build SpanId if NOT sampled'() {
        setup:
            request.getHttpHeaderValue(BraveHttpHeaders.Sampled.getName()) >> "0"
        when:
            def traceData = adapter.getTraceData()
        then:
            traceData.getSpanId() == null
    }

    def 'should return span name from provider'() {
        setup:
            def spanName = "expected-span-name"
            spanNameProvider.spanName(request) >> spanName
        when:
            def result = adapter.getSpanName()
        then:
            result == spanName
    }
}
