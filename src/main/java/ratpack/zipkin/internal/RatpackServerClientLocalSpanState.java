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

import com.github.kristofa.brave.IdConversion;
import com.github.kristofa.brave.ServerClientAndLocalSpanState;
import com.github.kristofa.brave.ServerSpan;
import com.twitter.zipkin.gen.Endpoint;
import com.twitter.zipkin.gen.Span;
import org.slf4j.MDC;
import ratpack.exec.Execution;
import ratpack.registry.MutableRegistry;

import java.util.function.Supplier;


public class RatpackServerClientLocalSpanState implements ServerClientAndLocalSpanState  {
  public static final String MDC_SERVER_SPAN_ID = "serverSpan.id";
  public static final String MDC_SERVICE_NAME = "service.name";
  public static final String MDC_TRACE_ID = "traceId";
  public static final String MDC_PARENT_SPAN_ID = "parentSpan.id";

  private final Supplier<MutableRegistry> registry;
  private final MDCProxy mdc;
  private final Endpoint endpoint;

  public RatpackServerClientLocalSpanState(final Endpoint endpoint,
                                           final Supplier<MutableRegistry> registry,
                                           final MDCProxy mdc) {
    this.registry = registry;
    this.endpoint = endpoint;
    this.mdc = mdc;
  }

  public RatpackServerClientLocalSpanState(final Endpoint endpoint) {
    this(endpoint, Execution::current, new DefaultMDCProxyImpl());
  }

  @Override
  public void setCurrentClientSpan(final Span span) {
    registry.get().add(new CurrentClientSpanValue(span));
  }

  @Override
  public Span getCurrentClientSpan() {

    return registry.get()
        .maybeGet(CurrentClientSpanValue.class)
        .map(TypedValue::get)
        .orElseGet(() -> {
          if (Execution.isManagedThread()) {
            return Execution.current()
                    .maybeGet(CurrentClientSpanValue.class)
                    .map(TypedValue::get)
                    .orElse(null);
          } else {
            return null;
          }
        });
  }

  @Override
  public Span getCurrentLocalSpan() {
    return registry.get()
        .maybeGet(CurrentLocalSpanValue.class)
        .map(TypedValue::get)
        .orElseGet(() -> {
          if (Execution.isManagedThread()) {
            return Execution.current()
                    .maybeGet(CurrentLocalSpanValue.class)
                    .map(TypedValue::get)
                    .orElse(null);
          } else {
            return null;
          }
        });
  }

  @Override
  public void setCurrentLocalSpan(final Span span) {
    registry.get().add(new CurrentLocalSpanValue(span));
  }

  @Override
  public ServerSpan getCurrentServerSpan() {
    return registry.get()
        .maybeGet(CurrentServerSpanValue.class)
        .map(TypedValue::get)
        .orElseGet(() -> {
          if (Execution.isManagedThread()) {
            return Execution.current()
                    .maybeGet(CurrentServerSpanValue.class)
                    .map(TypedValue::get)
                    .orElse(ServerSpan.EMPTY);
          } else {
            return ServerSpan.EMPTY;
          }
        });
  }

  @Override
  public void setCurrentServerSpan(final ServerSpan serverSpan) {
    if (serverSpan != null) {
      Span span = serverSpan.getSpan();
      if (span != null) {
        mdc.put(MDC_SERVER_SPAN_ID, IdConversion.convertToString(span.getId()));
        mdc.put(MDC_TRACE_ID, IdConversion.convertToString(span.getTrace_id()));
        if (span.getParent_id() != null) {
          mdc.put(MDC_PARENT_SPAN_ID, IdConversion.convertToString(span.getParent_id()));
        }
      }
    }
    registry.get().add(new CurrentServerSpanValue(serverSpan));
  }

  @Override
  public Boolean sample() {
    return getCurrentServerSpan().getSample();
  }

  @Override
  public Endpoint endpoint() {
    return endpoint;
  }

  private abstract static class TypedValue<T> {
    private T value;

    TypedValue(final T value) {
      this.value = value;
    }

    T get() {
      return value;
    }
  }

  public static class CurrentLocalSpanValue extends TypedValue<Span> {
    public CurrentLocalSpanValue(final Span value) {
      super(value);
    }
  }

  public static class CurrentClientSpanValue extends TypedValue<Span> {
    public CurrentClientSpanValue(final Span value) {
      super(value);
    }
  }

  public static class CurrentServerSpanValue extends TypedValue<ServerSpan> {
    public CurrentServerSpanValue(final ServerSpan value) {
      super(value);
    }
  }

  interface MDCProxy {
    void put(String key, String value);
  }

  private static class DefaultMDCProxyImpl implements MDCProxy {
    @Override
    public void put(final String key, final String value) {
      MDC.put(key, value);
    }
  }
}
