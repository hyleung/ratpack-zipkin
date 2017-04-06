package ratpack.zipkin.internal

import brave.propagation.CurrentTraceContext
import brave.propagation.TraceContext
import ratpack.registry.internal.SimpleMutableRegistry
import spock.lang.Specification


class RatpackCurrentTraceContextSpec extends Specification {
    CurrentTraceContext traceContext
    RatpackCurrentTraceContext.MDCProxy mdc
    def setup() {
        def registry = SimpleMutableRegistry.newInstance()
        mdc = Mock(RatpackCurrentTraceContext.MDCProxy)
        traceContext = new RatpackCurrentTraceContext({ registry }, mdc)
    }

    def "Should return null trace context if there isn't one"() {
        given:
            def result = traceContext.get()
        expect:
            result == null
    }

    def 'When creating a new scope, the trace context new'() {
        given:
            def next = Stub(TraceContext)
        when:
            def scope = traceContext.newScope(next)
        then:
            scope != null
    }

    def 'When a new scope is closed, the trace context should set back to the previous context'() {
        given:
            def initialContext = Stub(TraceContext)
        and:
            def scope = traceContext.newScope(initialContext)
        when:
            scope.close()
        then:
            traceContext.get() == null
    }

    def 'When creating a new scope with no parent span, should record ids in MDC'() {
        given:
            def next = TraceContext.newBuilder()
                            .traceId(1L)
                            .spanId(1l)
                            .build()
        when:
            traceContext.newScope(next)
        then:
            1 * mdc.put("X-B3-TraceId", _ as String)
            1 * mdc.put("X-B3-SpanId", _ as String)
    }

    def 'When creating a new scope with a parent span, should record ids in MDC'() {
        given:
            def next = TraceContext.newBuilder()
                    .traceId(1L)
                    .spanId(1l)
                    .parentId(1L)
                    .build()
        when:
            traceContext.newScope(next)
        then:
            1 * mdc.put("X-B3-TraceId", _ as String)
            1 * mdc.put("X-B3-SpanId", _ as String)
            1 * mdc.put("X-B3-ParentSpanId", _ as String)
    }
}
