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
import ratpack.func.Function
import ratpack.http.Request
import spock.lang.Specification

class RequestAnnotationExtractorSpec extends Specification {
    def 'Extractor should delegate to function'() {
        setup:
            def Function<Request, Collection<KeyValueAnnotation>> func = Mock(Function)
            def Request request = Mock(Request)
            def extractor = new RequestAnnotationExtractor(func)
        when:
            extractor.annotationsFrom(request)
        then:
            1 * func.apply(request)
    }
}

