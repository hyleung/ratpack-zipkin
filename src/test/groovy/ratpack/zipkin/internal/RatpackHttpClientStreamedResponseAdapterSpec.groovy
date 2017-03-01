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

import com.github.kristofa.brave.ClientResponseAdapter
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.StreamedResponse
import ratpack.http.internal.DefaultStatus
import spock.genesis.Gen
import spock.lang.Specification
import zipkin.Constants
import zipkin.TraceKeys

class RatpackHttpClientStreamedResponseAdapterSpec extends Specification {
    def StreamedResponse response = Stub(StreamedResponse)
    def ClientResponseAdapter adapter

    def void setup() {
        adapter = new RatpackHttpClientStreamedResponseAdapter(response)
    }

    def 'Should not return response annotation for 2xx status'(int statusCode) {
        setup:
        response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
        def result = adapter.responseAnnotations()
        then:
        result.isEmpty()
        where:
        statusCode << Gen.integer(200..299).take(10)

    }

    def 'Should return annotations for status (< 2xx)'(int statusCode) {
        setup:
        response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
        def result = adapter.responseAnnotations()
        then:
        !result.isEmpty()
        def entry = result.find {annotation -> annotation.getKey() == TraceKeys.HTTP_STATUS_CODE}
        entry.getValue() == statusCode.toString()
        where:
        statusCode << Gen.integer(100..199).take(10)
    }

    def 'Should return annotations for status (>= 3xx)'(int statusCode) {
        setup:
        response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
        def result = adapter.responseAnnotations()
        then:
        !result.isEmpty()
        def entry = result.find {annotation -> annotation.getKey() == TraceKeys.HTTP_STATUS_CODE}
        entry.getValue() == statusCode.toString()
        where:
        statusCode << Gen.integer(300..400).take(10)
    }

    def 'Should return error annotations for status (>= 5xx)'(int statusCode) {
        setup:
        response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
        def result = adapter.responseAnnotations()
        then:
        !result.isEmpty()
        def entry = result.find {annotation -> annotation.getKey() == Constants.ERROR}
        entry.getValue() == "error status " + statusCode
        where:
        statusCode << Gen.integer(500..550).take(10)
    }

    def 'Should return error annotation when missing status code'() {
        setup:
        response.getStatus() >>  null
        when:
        def result = adapter.responseAnnotations()
        then:
        !result.isEmpty()
        def entry = result.find {annotation -> annotation.getKey() == Constants.ERROR}
        entry.getValue() == "missing or unknown status code"
        where:
        statusCode << Gen.integer(200..299).take(10)
    }
}
