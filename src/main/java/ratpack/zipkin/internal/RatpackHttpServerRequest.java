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

import com.github.kristofa.brave.http.HttpServerRequest;
import ratpack.http.Request;

import java.net.URI;

/**
 * Created by hyleung on 2016-04-05.
 */
public class RatpackHttpServerRequest implements HttpServerRequest {
  private final Request request;

  public RatpackHttpServerRequest(final Request request) {
    this.request = request;
  }

  @Override
  public String getHttpHeaderValue(final String headerName) {
    return request.getHeaders().get(headerName);
  }

  @Override
  public URI getUri() {
    return URI.create(request.getUri());
  }

  @Override
  public String getHttpMethod() {
    return request.getMethod().getName();
  }
}
