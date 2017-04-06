package ratpack.zipkin.internal;

import brave.propagation.CurrentTraceContext;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import org.slf4j.MDC;
import ratpack.exec.Execution;
import ratpack.registry.MutableRegistry;

import java.util.function.Supplier;

public class RatpackCurrentTraceContext extends CurrentTraceContext {

  private final Supplier<MutableRegistry> registrySupplier;
  private final MDCProxy mdc;
  private final TraceContext.Injector<MDCProxy> contextInjector = Propagation.B3_STRING.injector(MDCProxy::put);
  RatpackCurrentTraceContext(final Supplier<MutableRegistry> registrySupplier,
                             final MDCProxy mdc) {
    this.registrySupplier = registrySupplier;
    this.mdc = mdc;
    this.registrySupplier.get().add(new TraceContextHolder(null));
  }

  public RatpackCurrentTraceContext() {
    this(Execution::current, new DefaultMDCProxyImpl());
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
    contextInjector.inject(currentSpan, this.mdc);
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
