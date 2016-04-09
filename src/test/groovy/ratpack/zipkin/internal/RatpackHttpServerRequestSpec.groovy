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

import com.github.kristofa.brave.http.HttpServerRequest
import ratpack.http.HttpMethod
import ratpack.http.Request
import spock.lang.Specification

/**
 * Test suite for {@link RatpackHttpServerRequest}
 */
class RatpackHttpServerRequestSpec extends Specification {
    def Request request = Stub(Request)

    def HttpServerRequest httpServerRequest
    def setup() {
        httpServerRequest = new RatpackHttpServerRequest(request)
    }
    def 'HttpServerRequest should get URI'() {
        setup:
            def uri = "http://foo.com"
            request.getUri() >> uri
        when:
            def result = httpServerRequest.getUri()
        then:
            URI.create(uri) == result
    }
    def 'HttpServerRequest throws IllegalArgument for malformed URI'() {
        setup:
            def uri = "malformed uri"
            request.getUri() >> uri
        when:
            httpServerRequest.getUri()
        then:
            thrown(IllegalArgumentException)
    }
    def 'HttpServerRequest should return HTTP method'() {
        setup:
            def method = Stub(HttpMethod)
            request.getMethod() >> method
            method.getName() >> "GET"
        when:
            def result = httpServerRequest.getHttpMethod()
        then:
            result == "GET"
    }
}
