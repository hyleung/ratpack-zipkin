package ratpack.zipkin;

import brave.propagation.TraceContext;
import java.util.Arrays;
import ratpack.exec.Promise;
import ratpack.exec.util.ParallelBatch;
import ratpack.zipkin.internal.RatpackCurrentTraceContext.TraceContextTypeValue;

public class TracedParallelBatch {

  /**
   * Add {@link TraceContext} propagation to {@link ParallelBatch#of(Promise[])}.
   *
   * Example usage:
   *
   * TracedParallelBatch
   *   .of(tracer.currentSpan().context(), promises)
   *   .yield();
   *
   * @param traceContext
   * @param promises
   * @param <T>
   * @return
   */
  static <T> ParallelBatch<T> of(final TraceContext traceContext, Iterable<? extends Promise<T>> promises) {
    return ParallelBatch.of(promises)
        .execInit(execution -> execution.add(new TraceContextTypeValue(traceContext)));
  }

  static <T> ParallelBatch<T> of(final TraceContext traceContext, Promise<T>... promises) {
    return of(traceContext, Arrays.asList(promises));
  }

}
