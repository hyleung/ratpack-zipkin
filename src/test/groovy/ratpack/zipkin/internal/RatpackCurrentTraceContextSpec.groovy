package ratpack.zipkin.internal

import brave.propagation.CurrentTraceContext
import brave.propagation.TraceContext
import ratpack.registry.MutableRegistry
import ratpack.registry.Registry
import spock.lang.Specification

class RatpackCurrentTraceContextSpec extends Specification {
    MutableRegistry registry = Registry.mutable()
    CurrentTraceContext traceContext

    def setup() {
        traceContext = new RatpackCurrentTraceContext({ -> registry})
    }

    def dummyContext() {
        TraceContext
            .newBuilder()
            .traceId(new Random().nextLong())
            .spanId(new Random().nextLong())
            .build()
    }

    def 'Initial context should be null'() {
        given:
            def current = traceContext.get()
        expect:
            current == null
    }

    def 'When setting TraceContext span, should return same TraceContext'() {
        given:
            def expected = dummyContext()
        and:
            traceContext.newScope(expected)
        when:
            def result = traceContext.get()
        then:
            result == expected
    }

    def 'When closing a scope, trace context should revert back to previous'() {
        given:
            traceContext.newScope(dummyContext())
            def expected = dummyContext()
            traceContext.newScope(expected)
        and:
            def scope = traceContext.newScope(dummyContext())
        when:
            scope.close()
            def traceContext = traceContext.get()
        then:
            expected ==  traceContext
    }


    def 'When closing a scope, trace context should revert back to previous until null'() {
        given:
            def scope_1 = traceContext.newScope(dummyContext())
            def scope_2 = traceContext.newScope(dummyContext())
            def scope_3 = traceContext.newScope(dummyContext())
        when:
            scope_3.close()
            scope_2.close()
            scope_1.close()
            def traceContext = traceContext.get()
        then:
            traceContext == null
    }

    def 'When TraceContext is null the context should be cleared'() {
        given:
            def expected = dummyContext()
        and:
            traceContext.newScope(expected)
        when:
            def result = traceContext.get()
        then:
            result == expected
        when:
            traceContext.newScope(null)
        then:
            traceContext.get() == null
    }

}
