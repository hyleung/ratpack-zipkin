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

import com.github.kristofa.brave.ClientRequestAdapter
import com.github.kristofa.brave.KeyValueAnnotation
import com.github.kristofa.brave.SpanId
import com.github.kristofa.brave.http.HttpClientRequest
import com.github.kristofa.brave.http.ServiceNameProvider
import com.github.kristofa.brave.http.SpanNameProvider
import ratpack.http.MutableHeaders
import ratpack.http.client.RequestSpec
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

/**
 * Test suite for {@link RatpackClientRequestAdapter}.
 *
 * Behaviour is based on source for {@link com.github.kristofa.brave.http.HttpClientRequestAdapter}.
 */
class RatpackClientRequestAdapterSpec extends Specification {
    def ServiceNameProvider serviceNameProvider = Stub(ServiceNameProvider)
    def SpanNameProvider spanNameProvider = Stub(SpanNameProvider)
    def RequestSpec requestSpec = Mock(RequestSpec)
    def MutableHeaders headers = Mock(MutableHeaders)
    def ClientRequestAdapter adapter

    def setup() {
        adapter = new RatpackClientRequestAdapter(requestSpec, "GET", serviceNameProvider, spanNameProvider)
        requestSpec.getHeaders() >> headers
    }

    def 'Should get span name from provider'() {
        given:
            def expected = "some-span-name"
            spanNameProvider.spanName(_ as HttpClientRequest) >> expected
        expect:
            adapter.getSpanName() == expected
    }

    def 'Should get client service name from provider'() {
        given:
            def expected = "some-service-name"
            serviceNameProvider.serviceName(_ as HttpClientRequest) >> expected
        expect:
            adapter.getClientServiceName() == expected
    }

    def 'Should return uri in annotations'() {
        given:
            def expected = new URI("some-uri")
            requestSpec.getUrl() >> expected
        when:
            def result = adapter.requestAnnotations()
        then:
            assertThat(result).contains(KeyValueAnnotation.create("http.uri", expected.toString()))
    }

    def 'When span id is null, should add "X-B3-Sampled" header with value 0'() {
        when:
            adapter.addSpanIdToRequest(null)
        then:
            1 * headers.add("X-B3-Sampled", "0")
    }

    def 'When span id is NOT null, should add "X-B3-Sampled" header with value 1'() {
        when:
            adapter.addSpanIdToRequest(Mock(SpanId))
        then:
            1 * headers.add("X-B3-Sampled", "1")
    }

    def 'When span id present, should add "X-B3-SpanId" header'() {
        setup:
            def spanId = SpanId.create(1l, 2l, null)
        when:
            adapter.addSpanIdToRequest(spanId)
        then:
            1 * headers.add("X-B3-SpanId", _ as String)
    }

    def 'When span id present, should add "X-B3-TraceId" header'() {
        setup:
            def spanId = SpanId.create(1l, 2l, null)
        when:
            adapter.addSpanIdToRequest(spanId)
        then:
            1 * headers.add("X-B3-TraceId", _ as String)
    }

    def 'When parent span id present, should add "X-B3-ParentSpanId" header'() {
        setup:
            def spanId = SpanId.create(1l, 2l, new Long(1l))
        when:
            adapter.addSpanIdToRequest(spanId)
        then:
            1 * headers.add("X-B3-ParentSpanId", _ as String)
    }

    def 'When parent span id NOT present, should NOT add "X-B3-ParentSpanId" header'() {
        setup:
            def spanId = SpanId.create(1l, 2l, null)
        when:
            adapter.addSpanIdToRequest(spanId)
        then:
            0 * headers.add("X-B3-ParentSpanId", _ as String)
    }
}
