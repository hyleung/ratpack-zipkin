package ratpack.zipkin.internal

import brave.propagation.CurrentTraceContext
import brave.propagation.TraceContext
import ratpack.registry.internal.SimpleMutableRegistry
import spock.lang.Specification


class RatpackCurrentTraceContextSpec extends Specification {
    CurrentTraceContext traceContext

    def setup() {
        def registry = SimpleMutableRegistry.newInstance()
        traceContext = new RatpackCurrentTraceContext({ registry })
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
}
