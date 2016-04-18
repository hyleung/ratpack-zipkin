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

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerResponseAdapter;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.http.Response;
import ratpack.zipkin.ResponseAnnotationExtractor;

import java.util.Collection;

/**
 * Implementation of {@link ServerResponseAdapter} for RatPack.
 */
class RatpackServerResponseAdapter implements ServerResponseAdapter {
  private final Logger logger = LoggerFactory.getLogger(RatpackServerResponseAdapter.class);
  private final Response response;
  private final ResponseAnnotationExtractor extractor;

  RatpackServerResponseAdapter(final Response response,
                               final ResponseAnnotationExtractor extractor) {
    this.response = response;
    this.extractor = extractor;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<KeyValueAnnotation> responseAnnotations() {
    Collection<KeyValueAnnotation> annotations = Lists.newArrayList();
    try {
      Collection<KeyValueAnnotation> extracted = extractor.annotationsForResponse(response);
      if (extracted != null) {
        annotations.addAll(extracted);
      }
    } catch (Exception e) {
      logger.error("Failed to extract annotations from Response: {}", e);
    }

    logger.debug("Response annotations: {}", annotations);
    return annotations;

  }
}
