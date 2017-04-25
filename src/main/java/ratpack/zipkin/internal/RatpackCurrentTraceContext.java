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

  @Override
  public TraceContext get() {
    return registrySupplier.get()
        .maybeGet(TraceContextHolder.class)
        .map(TraceContextHolder::getContext)
        .orElse(null);
  }

  @Override
  public Scope newScope(TraceContext currentSpan) {
    final TraceContext previous = get();
    registrySupplier.get().add(TraceContextHolder.class, new TraceContextHolder(currentSpan));
    return () -> registrySupplier.get().add(new TraceContextHolder(previous));
  }

  public static final class TraceContextHolder {
    private final TraceContext context;
    public TraceContextHolder(final TraceContext context) {
      this.context = context;
    }
    public TraceContext getContext() {
      return this.context;
    }
  }

}
