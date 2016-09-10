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

import com.github.kristofa.brave.http.SpanNameProvider
import ratpack.http.client.RequestSpec
import spock.lang.Specification

class ClientRequestAdapterFactorySpec extends Specification {
    def SpanNameProvider spanNameProvider = Stub(SpanNameProvider)

    def 'should create client request adapter'() {
        given:
            def factory = new ClientRequestAdapterFactory(spanNameProvider)
        when:
            def adapter = factory.createAdaptor(Stub(RequestSpec), "GET")
        then:
            adapter != null
    }

}
