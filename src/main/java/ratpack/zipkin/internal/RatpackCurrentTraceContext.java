package ratpack.zipkin.internal;

import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import java.util.function.Supplier;
import ratpack.exec.Execution;
import ratpack.registry.MutableRegistry;

public final class RatpackCurrentTraceContext extends CurrentTraceContext {

  private final Supplier<MutableRegistry> registrySupplier;

  public RatpackCurrentTraceContext(Supplier<MutableRegistry> registrySupplier) {
    this.registrySupplier = registrySupplier;
  }

  public RatpackCurrentTraceContext() {
    this(Execution::current);
  }

  @Override public TraceContext get() {
    return registrySupplier.get()
        .maybeGet(TraceContextTypeValue.class)
        .map(TypedValue::get)
        .orElse(null);
  }

  @Override public Scope newScope(TraceContext currentSpan) {
    final TraceContext previous = get();
    registrySupplier.get().add(new TraceContextTypeValue(currentSpan));
    return () -> registrySupplier.get().add(new TraceContextTypeValue(previous));
  }

  private class TraceContextTypeValue extends TypedValue<TraceContext> {
    TraceContextTypeValue(final TraceContext value) {
      super(value);
    }
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

}
