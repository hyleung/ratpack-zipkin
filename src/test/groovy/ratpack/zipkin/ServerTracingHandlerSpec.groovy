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
package ratpack.zipkin

import com.github.kristofa.brave.KeyValueAnnotation
import com.github.kristofa.brave.ServerRequestAdapter
import com.github.kristofa.brave.ServerRequestInterceptor
import com.github.kristofa.brave.ServerResponseAdapter
import com.github.kristofa.brave.ServerResponseInterceptor
import com.github.kristofa.brave.http.SpanNameProvider
import ratpack.func.Action
import ratpack.func.Function
import ratpack.handling.Context
import ratpack.http.Request
import ratpack.http.Response
import ratpack.zipkin.internal.ServerRequestAdapterFactory
import ratpack.zipkin.internal.ServerResponseAdapterFactory
import spock.lang.Specification

/**
 * Test suite for {@link ServerTracingHandler}.
 */
class ServerTracingHandlerSpec extends Specification{

    def ServerRequestInterceptor requestInterceptor = Mock(ServerRequestInterceptor)

    def ServerResponseInterceptor responseInterceptor = Mock(ServerResponseInterceptor)

    def ServerRequestAdapterFactory requestAdapterFactory = Mock(ServerRequestAdapterFactory)

    def ServerResponseAdapterFactory responseAdapterFactory = Mock(ServerResponseAdapterFactory)

    def SpanNameProvider spanNameProvider = Mock(SpanNameProvider)

    def  Function<Request, Collection<KeyValueAnnotation>> requestAnnotationExtractor = Mock(Function)

    def Context ctx = Mock(Context)
    def Request request = Mock(Request)
    def Response response = Mock(Response)

    ServerTracingHandler handler

    def setup() {
        handler = new ServerTracingHandler(requestInterceptor,
                responseInterceptor,
                requestAdapterFactory,
                responseAdapterFactory,
                spanNameProvider,
                requestAnnotationExtractor)
        requestAdapterFactory.createAdapter(spanNameProvider, request, requestAnnotationExtractor) >> Mock(ServerRequestAdapter)
        responseAdapterFactory.createAdapter(response) >> Mock(ServerResponseAdapter)
        requestAnnotationExtractor.apply(_) >> Collections.emptyList()
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
