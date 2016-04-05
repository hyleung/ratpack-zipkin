package ratpack.zipkin

import com.github.kristofa.brave.ServerRequestAdapter
import com.github.kristofa.brave.ServerRequestInterceptor
import com.github.kristofa.brave.ServerResponseAdapter
import com.github.kristofa.brave.ServerResponseInterceptor
import com.github.kristofa.brave.http.ServiceNameProvider
import com.github.kristofa.brave.http.SpanNameProvider
import ratpack.func.Action
import ratpack.handling.Context
import ratpack.http.Request
import ratpack.http.Response
import spock.lang.Specification


/**
 * Created by hyleung on 2016-04-04.
 */
class ServerTracingHandlerSpec extends Specification{

    def ServerRequestInterceptor requestInterceptor = Mock(ServerRequestInterceptor)

    def ServerResponseInterceptor responseInterceptor = Mock(ServerResponseInterceptor)

    def ServiceNameProvider serviceNameProvider = Mock(ServiceNameProvider)

    def SpanNameProvider spanNameProvider = Mock(SpanNameProvider)

    def Context ctx = Mock(Context)
    def Request request = Mock(Request)
    def Response response = Mock(Response)

    ServerTracingHandler handler

    def setup() {
        handler = new ServerTracingHandler(requestInterceptor, responseInterceptor, serviceNameProvider, spanNameProvider)
    }

    def 'Given a server request, should handle with ServerRequestInterceptor'() {
        setup:
            ctx.getRequest() >> request
            ctx.getResponse() >> response
        when:
            handler.handle(ctx)
        then:
            requestInterceptor.handle(_ as ServerRequestAdapter)
    }
    def 'Given a server request, should invoke next handler in chain'() {
        setup:
            ctx.getRequest() >> request
            ctx.getResponse() >> response
        when:
            handler.handle(ctx)
        then:
             ctx.next()
    }
    def 'Given a response, should handle with ServerResponseInterceptor'() {
        setup:
            ctx.getRequest() >> request
            ctx.getResponse() >> response
            response.beforeSend(_) >> { Action<? super Response> callback -> callback.execute(response) }
        when:
            handler.handle(ctx)
        then:
            responseInterceptor.handle(_ as ServerResponseAdapter)

    }
}
