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

import com.github.kristofa.brave.ServerClientAndLocalSpanState;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.internal.Nullable;
import com.twitter.zipkin.gen.Endpoint;
import com.twitter.zipkin.gen.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Execution;
import ratpack.registry.MutableRegistry;


public class RatpackServerClientLocalSpanState implements ServerClientAndLocalSpanState  {
  private final MutableRegistry registry;

  public RatpackServerClientLocalSpanState(final String serviceName, int ip, int port) {
    this(serviceName, ip, port, Execution.current());
  }

  RatpackServerClientLocalSpanState(final String serviceName, int ip, int port, final MutableRegistry registry) {
    this.registry = registry;
    State state = new State();
    state.setServerEndpoint(Endpoint.create(serviceName, ip, port));
    this.registry.add(state);
  }

  @Override
  public Span getCurrentClientSpan() {
    return getState().getClientSpan();
  }

  @Override
  public Endpoint getClientEndpoint() {
    return getState().getClientEndpoint();
  }

  @Override
  public void setCurrentClientSpan(final Span span) {
    State state = getState();
    state.setClientSpan(span);
    putState(state);
  }

  @Override
  public void setCurrentClientServiceName(@Nullable final String serviceName) {
    State state = getState();
    if (serviceName == null) {
      state.setClientEndpoint(state.getServerEndpoint());
    } else {
      Endpoint serverEndPoint = getServerEndpoint();
      Endpoint endpoint = Endpoint.create(serviceName, serverEndPoint.ipv4, serverEndPoint.port);
      state.setClientEndpoint(endpoint);
    }
    putState(state);
  }

  @Override
  public Span getCurrentLocalSpan() {
    return getState().getLocalSpan();
  }

  @Override
  public void setCurrentLocalSpan(final Span span) {
    State state = getState();
    state.setLocalSpan(span);
    putState(state);
  }

  @Override
  public ServerSpan getCurrentServerSpan() {
    return getState().getServerSpan();
  }

  @Override
  public Endpoint getServerEndpoint() {
    return getState().getServerEndpoint();
  }

  @Override
  public void setCurrentServerSpan(final ServerSpan span) {
    State state = getState();
    state.setServerSpan(span);
    putState(state);
  }

  @Override
  public Boolean sample() {
    return getCurrentServerSpan().getSample();
  }

  private void putState(final State state) {
    registry.remove(State.class);
    registry.add(state);
  }

  private State getState() {
    return registry.get(State.class);
  }

  static class State {
    private Span localSpan;
    private ServerSpan serverSpan;
    private Span clientSpan;
    private Endpoint serverEndpoint;
    private Endpoint clientEndpoint;

    Span getLocalSpan() {
      return localSpan;
    }

    void setLocalSpan(final Span localSpan) {
      this.localSpan = localSpan;
    }

    ServerSpan getServerSpan() {
      return serverSpan;
    }

    void setServerSpan(final ServerSpan serverSpan) {
      this.serverSpan = serverSpan;
    }

    Span getClientSpan() {
      return clientSpan;
    }

    void setClientSpan(final Span clientSpan) {
      this.clientSpan = clientSpan;
    }

    Endpoint getServerEndpoint() {
      return serverEndpoint;
    }

    void setServerEndpoint(final Endpoint serverEndpoint) {
      this.serverEndpoint = serverEndpoint;
    }

    Endpoint getClientEndpoint() {
      return clientEndpoint;
    }

    void setClientEndpoint(final Endpoint clientEndpoint) {
      this.clientEndpoint = clientEndpoint;
    }
  }
}
