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
package ratpack.zipkin.internal;

import com.github.kristofa.brave.ServerResponseAdapter;
import ratpack.http.Response;
import ratpack.zipkin.ResponseAnnotationExtractor;

import javax.inject.Inject;

/**
 * This class is responsible for creating {@link ServerResponseAdapter} instances.
 */
public class ServerResponseAdapterFactory {
  private final ResponseAnnotationExtractor extractor;

  @Inject
  public ServerResponseAdapterFactory(final ResponseAnnotationExtractor extractor) {
    this.extractor = extractor;
  }

  /**
   * Create a {@link ServerResponseAdapter} instance.
   *
   * @param response a response
   * @return an adapter
   */
  public ServerResponseAdapter createAdapter(final Response response) {
    return new RatpackServerResponseAdapter(response, extractor);
  }
}
