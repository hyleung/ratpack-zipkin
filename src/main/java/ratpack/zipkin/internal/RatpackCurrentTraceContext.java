package ratpack.zipkin.internal;

import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import ratpack.exec.Execution;
import ratpack.registry.MutableRegistry;

import java.util.function.Supplier;

public class RatpackCurrentTraceContext extends CurrentTraceContext {

  final Supplier<MutableRegistry> registrySupplier;

  RatpackCurrentTraceContext(final Supplier<MutableRegistry> registrySupplier) {
    this.registrySupplier = registrySupplier;
    this.registrySupplier.get().add(new TraceContextHolder(null));
  }

  public RatpackCurrentTraceContext() {
    this(Execution::current);
  }

  @Override
  public TraceContext get() {
    return registrySupplier
        .get()
        .get(TraceContextHolder.class)
        .getContext();

  }

  @Override
  public Scope newScope(final TraceContext currentSpan) {
    final TraceContext previous = get();
    registrySupplier
        .get()
        .add(TraceContextHolder.class, new TraceContextHolder(currentSpan));
    return () -> registrySupplier.get().add(TraceContextHolder.class, new TraceContextHolder(previous));
  }

  private static class TraceContextHolder {
    private final TraceContext context;

    public TraceContextHolder(final TraceContext context) {
      this.context = context;
    }

    public TraceContext getContext() {
      return context;
    }
  }
}
