package ratpack.zipkin.internal;

import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import ratpack.exec.Execution;
import ratpack.registry.MutableRegistry;

import java.util.function.Supplier;

public class RatpackCurrentTraceContext extends CurrentTraceContext {

  final Supplier<MutableRegistry> registry;

  RatpackCurrentTraceContext(final Supplier<MutableRegistry> registry) {
    this.registry = registry;
  }

  public RatpackCurrentTraceContext() {
    this(Execution::current);
  }

  @Override
  public TraceContext get() {
    throw new RuntimeException("Not implemented!");
  }

  @Override
  public Scope newScope(final TraceContext currentSpan) {
    throw new RuntimeException("Not implemented!");
  }
}
