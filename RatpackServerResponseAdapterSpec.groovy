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
import com.github.kristofa.brave.ServerResponseAdapter
import ratpack.http.Response
import ratpack.registry.Registry
import ratpack.registry.internal.SimpleMutableRegistry
import ratpack.zipkin.ResponseAnnotationExtractor
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

/**
 * Test suite for {@link RatpackServerResponseAdapter}.
 */
class RatpackServerResponseAdapterSpec extends Specification {
    def Response response = Stub(Response)
    def ResponseAnnotationExtractor extractor = Mock(ResponseAnnotationExtractor)
    def ServerResponseAdapter responseAdapter

    def setup() {
        responseAdapter = new RatpackServerResponseAdapter(response, extractor, { Stub(Registry) })
    }

    def 'Should include annotations from extractor function'() {
        setup:
            def expected = KeyValueAnnotation.create("foo", "bar")
            extractor.annotationsForResponse(response) >> Collections.singleton(expected)
        when:
            def Collection<KeyValueAnnotation> result = responseAdapter.responseAnnotations()
        then:
            assertThat(result)
                .contains(expected)
    }

    def 'Should return if extractor returns null'() {
        setup:
            extractor.annotationsForResponse(_) >> null
        when:
            def result = responseAdapter.responseAnnotations()
        then:
            result.isEmpty()
    }

    def 'Should return if extractor errors'() {
        setup:
            extractor.annotationsForResponse(_) >> new IllegalArgumentException()
        when:
            def result = responseAdapter.responseAnnotations()
        then:
            result.isEmpty()
    }

    def 'Should include any KeyValueAnnotations in the registry'() {
        setup:
            def expected = KeyValueAnnotation.create("foo", "bar")
            def registry = SimpleMutableRegistry.single(expected)
            responseAdapter = new RatpackServerResponseAdapter(response, extractor, { registry })
        when:
            def result = responseAdapter.responseAnnotations()
        then:
            assertThat(result).contains(expected)
    }
}
