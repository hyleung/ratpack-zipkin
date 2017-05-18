package ratpack.zipkin;

import brave.propagation.TraceContext;
import java.util.Arrays;
import ratpack.exec.Promise;
import ratpack.exec.util.ParallelBatch;
import ratpack.zipkin.internal.RatpackCurrentTraceContext;

public final class TracedParallelBatch<T> {

  private final Iterable<? extends Promise<T>> promises;

  private TracedParallelBatch(Iterable<? extends Promise<T>> promises) {
    this.promises = promises;
  }

  static <T> TracedParallelBatch<T> of(Iterable<? extends Promise<T>> promises) {
    return new TracedParallelBatch<>(promises);
  }

  static <T> TracedParallelBatch<T> of(Promise<T>... promise) {
    return new TracedParallelBatch<>(Arrays.asList(promise));
  }

  static <T> ParallelBatch<T> of(final TraceContext context, Iterable<? extends Promise<T>> promises) {
    return ParallelBatch.of(promises)
        .execInit(execution ->
            execution.add(new RatpackCurrentTraceContext.TraceContextHolder(context)));
  }

  static <T> ParallelBatch<T> of(final TraceContext context, Promise<T>... promise) {
    return of(context, Arrays.asList(promise));
  }

  public ParallelBatch<T> withContext(final TraceContext context) {
    return of(context, promises);
  }

}
