/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ratpack.zipkin.internal

import com.github.kristofa.brave.IdConversion
import com.github.kristofa.brave.ServerRequestAdapter
import com.github.kristofa.brave.http.BraveHttpHeaders
import com.github.kristofa.brave.http.SpanNameProvider
import ratpack.http.Headers
import ratpack.http.Request
import ratpack.zipkin.RequestAnnotationExtractor
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assert.assertEquals

/**
 * Test suite for {@link RatpackServerRequestAdapter}.
 */
class RatpackServerRequestAdapterSpec extends Specification {
    def Request request = Stub(Request)
    def SpanNameProvider spanNameProvider = Stub(SpanNameProvider)
    def RequestAnnotationExtractor extractor = Mock(RequestAnnotationExtractor)
    def Headers headers  = Stub(Headers)
    ServerRequestAdapter adapter
    def setup() {
        adapter = new RatpackServerRequestAdapter(spanNameProvider, request, extractor)
        request.getHeaders() >> headers
    }

    @Unroll
    def 'getTraceData should set sampled flag with header "X-B3-Sampled = #headerValue"'(String headerValue,
                                                                                       boolean expected) {
        setup:
            headers.get(BraveHttpHeaders.Sampled.getName()) >> headerValue
            def parentId = "7b"
            def traceId = "1c8"
            def spanId = "315"
            headers.get(BraveHttpHeaders.ParentSpanId.getName()) >> parentId
            headers.get(BraveHttpHeaders.TraceId.getName()) >> traceId
            headers.get(BraveHttpHeaders.SpanId.getName()) >> spanId
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
            headers.get(BraveHttpHeaders.Sampled.getName()) >> "1"
            headers.get(BraveHttpHeaders.ParentSpanId.getName()) >> parentId
            headers.get(BraveHttpHeaders.TraceId.getName()) >> traceId
            headers.get(BraveHttpHeaders.SpanId.getName()) >> spanId
        when:
            def traceData = adapter.getTraceData()
            def span = traceData.getSpanId()
        then:
            span.nullableParentId() == IdConversion.convertToLong(parentId)
            span.@traceId == IdConversion.convertToLong(traceId)
            span.@spanId == IdConversion.convertToLong(spanId)
    }

    def 'getTraceData should build SpanId if parentId is absent'() {
        setup:
            def traceId = "1c8"
            def spanId = "315"
            headers.get(BraveHttpHeaders.Sampled.getName()) >> "1"
            headers.get(BraveHttpHeaders.ParentSpanId.getName()) >> null
            headers.get(BraveHttpHeaders.TraceId.getName()) >> traceId
            headers.get(BraveHttpHeaders.SpanId.getName()) >> spanId
        when:
            def traceData = adapter.getTraceData()
            def span = traceData.getSpanId()
        then:
            span.nullableParentId() == null
            span.@traceId == IdConversion.convertToLong(traceId)
            span.@spanId == IdConversion.convertToLong(spanId)
    }

    def 'getTraceData should NOT build SpanId if traceId header not present'() {
        setup:
            def parentId = "7b"
            def spanId = "315"
            headers.get(BraveHttpHeaders.Sampled.getName()) >> "1"
            headers.get(BraveHttpHeaders.ParentSpanId.getName()) >> parentId
            headers.get(BraveHttpHeaders.TraceId.getName()) >> null
            headers.get(BraveHttpHeaders.SpanId.getName()) >> spanId
        when:
            def traceData = adapter.getTraceData()
        then:
            traceData.getSpanId() == null
    }

    def 'getTraceData should NOT build SpanId if spanId header not present'() {
        setup:
            def parentId = "7b"
            def tracId = "315"
            headers.get(BraveHttpHeaders.Sampled.getName()) >> "1"
            headers.get(BraveHttpHeaders.ParentSpanId.getName()) >> parentId
            headers.get(BraveHttpHeaders.TraceId.getName()) >> tracId
            headers.get(BraveHttpHeaders.SpanId.getName()) >> null
        when:
            def traceData = adapter.getTraceData()
        then:
            traceData.getSpanId() == null
    }

    def 'getTraceData should NOT build SpanId if NOT sampled'() {
        setup:
            headers.get(BraveHttpHeaders.Sampled.getName()) >> "0"
        when:
            def traceData = adapter.getTraceData()
        then:
            traceData.getSpanId() == null
    }

    def 'Should return span name from provider'() {
        setup:
            def spanName = "expected-span-name"
            spanNameProvider.spanName(_) >> spanName
        when:
            def result = adapter.getSpanName()
        then:
            result == spanName
    }

    def 'Should get annotations via the extractor'() {
        when:
            adapter.requestAnnotations()
        then:
            1 * extractor.annotationsForRequest(request)
    }
    def 'Should return empty annotations if extractor throws exception'() {
        setup:
            extractor.annotationsForRequest(_) >> new IllegalArgumentException()
        when:
            def result = adapter.requestAnnotations()
        then:
            result.isEmpty()
    }
}
