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

import com.github.kristofa.brave.KeyValueAnnotation
import com.github.kristofa.brave.http.SpanNameProvider
import ratpack.func.Function
import ratpack.http.Request
import ratpack.zipkin.RequestAnnotationExtractor
import spock.lang.Specification

/**
 * Test suite for {@link ServerRequestAdapterFactory}.
 */
class ServerRequestAdapterFactorySpec extends Specification {
    def Request request = Mock(Request)
    def SpanNameProvider spanNameProvider = Mock(SpanNameProvider)
    def RequestAnnotationExtractor extractor = Mock(RequestAnnotationExtractor)
    def ServerRequestAdapterFactory factory;

    def setup() {
        factory = new ServerRequestAdapterFactory()
    }
    def 'Should build request adapter'() {
        when:
            def adapter = factory.createAdapter(spanNameProvider, request, extractor)
        then:
            adapter != null
    }
}
