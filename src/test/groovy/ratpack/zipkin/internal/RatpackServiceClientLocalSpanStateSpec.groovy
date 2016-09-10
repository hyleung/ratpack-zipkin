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

import com.github.kristofa.brave.ServerClientAndLocalSpanState
import com.github.kristofa.brave.ServerSpan

import com.twitter.zipkin.gen.Endpoint
import com.twitter.zipkin.gen.Span
import ratpack.registry.internal.SimpleMutableRegistry
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat
class RatpackServiceClientLocalSpanStateSpec extends Specification {
    def ServerClientAndLocalSpanState spanState
    def RatpackServerClientLocalSpanState.MDCProxy mdc = Mock(RatpackServerClientLocalSpanState.MDCProxy)
    def setup() {
        def registry = SimpleMutableRegistry.newInstance()
       spanState = new RatpackServerClientLocalSpanState("some-service-name", 0, 8080, { registry }, mdc)
    }

    def 'Should return server endpoint'() {
        given:
            def expectedServiceName = "expected-service-name"
            def expectedIp = 123
            def expectedPort = 1234
            spanState = new RatpackServerClientLocalSpanState(expectedServiceName,
                    expectedIp,
                    expectedPort,
                    {SimpleMutableRegistry.newInstance()},
                    mdc)

            def expected = Endpoint.create(expectedServiceName, expectedIp, expectedPort)
        when:
            def endpoint = spanState.endpoint()
        then:
            assertThat(endpoint).isEqualTo(expected)
    }

    def 'Should set server span to a value'() {
        setup:
            def serverSpan = Stub(ServerSpan)
        when:
            spanState.setCurrentServerSpan(serverSpan)
            def result = spanState.getCurrentServerSpan()
        then:
            result == serverSpan
    }

    def 'When setting server span should record server span id to MDC'() {
        setup:
            def serverSpan = Stub(ServerSpan)
        when:
            spanState.setCurrentServerSpan(serverSpan)
        then:
            1 * mdc.put(RatpackServerClientLocalSpanState.MDC_SERVER_SPAN_ID, _ as String)
    }

    def 'When setting server span should record server trace id to MDC'() {
        setup:
            def serverSpan = Stub(ServerSpan)
        when:
            spanState.setCurrentServerSpan(serverSpan)
        then:
            1 * mdc.put(RatpackServerClientLocalSpanState.MDC_TRACE_ID, _ as String)
    }

    def 'When setting server span should record parent span id to MDC if present'() {
        setup:
            def serverSpan = Stub(ServerSpan)
            def span = Stub(Span)
            serverSpan.getSpan() >> span
            span.getParent_id() >> new Long(1l)
        when:
            spanState.setCurrentServerSpan(serverSpan)
        then:
            1 * mdc.put(RatpackServerClientLocalSpanState.MDC_PARENT_SPAN_ID, _ as String)
    }

    def 'When setting server span should NOT record parent span id to MDC if parent id not present'() {
        setup:
            def serverSpan = Stub(ServerSpan)
            def span = Stub(Span)
            serverSpan.getSpan() >> span
            span.getParent_id() >> null
        when:
            spanState.setCurrentServerSpan(serverSpan)
        then:
            0 * mdc.put(RatpackServerClientLocalSpanState.MDC_PARENT_SPAN_ID, _ as String)
    }

    def 'Should initial server span to empty'() {
        given:
            def result = spanState.getCurrentServerSpan()
        expect:
            result == ServerSpan.EMPTY
    }

    def 'Should set client span to a value'() {
        setup:
            def clientSpan = Stub(Span)
        when:
            spanState.setCurrentClientSpan(clientSpan)
            def result = spanState.getCurrentClientSpan()
        then:
            result == clientSpan
    }

    def 'If no client span set should return null'() {
        given:
            def result = spanState.getCurrentClientSpan()
        expect:
            result == null
    }

    def 'Should get sampling from server span'() {
        setup:
            def serverSpan = Stub(ServerSpan)
            def expected = true
            serverSpan.getSample() >> expected
            spanState.setCurrentServerSpan(serverSpan)
        when:
            def result = spanState.sample()
        then:
            assertThat(result).isEqualTo(expected)
    }

    def 'Should setting local span to a value'() {
        setup:
            def span = Stub(Span)
        when:
            spanState.setCurrentLocalSpan(span)
            def result = spanState.getCurrentLocalSpan()
        then:
            result == span
    }
}
