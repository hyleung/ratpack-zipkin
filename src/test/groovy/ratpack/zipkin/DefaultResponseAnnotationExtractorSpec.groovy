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

import ratpack.http.Response
import ratpack.http.internal.DefaultStatus
import spock.genesis.Gen
import spock.lang.Specification
import zipkin.Constants
import zipkin.TraceKeys

class DefaultResponseAnnotationExtractorSpec extends Specification {

    def Response response = Stub(Response)
    def ResponseAnnotationExtractor extractor = ResponseAnnotationExtractor.DEFAULT

    def 'Should not return response annotation for 2xx status'(int statusCode) {
        setup:
            response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
            def result = extractor.annotationsForResponse(response)
        then:
            result.isEmpty()
        where:
            statusCode << Gen.integer(200..299).take(10)

    }

    def 'Should return annotations for status (< 2xx)'(int statusCode) {
        setup:
            response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
            def result = extractor.annotationsForResponse(response)
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
            def result = extractor.annotationsForResponse(response)
        then:
            !result.isEmpty()
            def entry = result.find {annotation -> annotation.getKey() == TraceKeys.HTTP_STATUS_CODE}
            entry.getValue() == statusCode.toString()
        where:
            statusCode << Gen.integer(300..500).take(10)
    }

    def 'Should return error annotations for status (>= 5xx)'(int statusCode) {
        setup:
        response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
        def result = extractor.annotationsForResponse(response)
        then:
        !result.isEmpty()
        def entry = result.find {annotation -> annotation.getKey() == Constants.ERROR}
        entry.getValue() == "server error"
        where:
        statusCode << Gen.integer(500..550).take(10)
    }
}
