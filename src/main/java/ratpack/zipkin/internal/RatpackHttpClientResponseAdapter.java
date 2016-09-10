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

import com.github.kristofa.brave.ClientResponseAdapter;
import com.github.kristofa.brave.KeyValueAnnotation;
import ratpack.http.client.ReceivedResponse;

import java.util.Collection;
import java.util.Collections;

class RatpackHttpClientResponseAdapter implements ClientResponseAdapter {
  private final ReceivedResponse response;

  RatpackHttpClientResponseAdapter(final ReceivedResponse response) {
    this.response = response;
  }

  @Override
  public Collection<KeyValueAnnotation> responseAnnotations() {
    int httpStatus = response.getStatus().getCode();

    if ((httpStatus < 200) || (httpStatus > 299)) {
      KeyValueAnnotation statusAnnotation = KeyValueAnnotation
          .create("http.responsecode", String.valueOf(httpStatus));
      return Collections.singleton(statusAnnotation);
    }
    return Collections.emptyList();
  }
}
