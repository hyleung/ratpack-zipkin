package ratpack.zipkin;

import brave.propagation.TraceContext;
import java.util.Arrays;
import ratpack.exec.Promise;
import ratpack.exec.util.ParallelBatch;
import ratpack.zipkin.internal.RatpackCurrentTraceContext;

/**
 * Provides factory methods for creating a RatPack {@link ParallelBatch}, with
 * the correct trace context.
 *
 * This is necessary because {@link ParallelBatch}es create forked executions
 * which do not "inherit" registries from the calling execution. Consequently,
 * the trace context must be passed explicitly to the forked executions.
 *
 * @param <T> the type of value produced by each promise in the batch.
 */
public final class TracedParallelBatch<T> {

  private final Iterable<? extends Promise<T>> promises;

  private TracedParallelBatch(Iterable<? extends Promise<T>> promises) {
    this.promises = promises;
  }

  /**
   * Create a {@link TracedParallelBatch} for list of {@link Promise}s.
   *
   * @param promises an iterable of Promises
   * @param <T> the type of value produced by each promise
   *
   * @return an instance of {@link TracedParallelBatch}.
   */
  public static <T> TracedParallelBatch<T> of(Iterable<? extends Promise<T>> promises) {
    return new TracedParallelBatch<>(promises);
  }

  /**
   * Create a {@link TracedParallelBatch} for list of {@link Promise}s.
   *
   * @param promises vararg containing a list of {@link Promise}s.
   * @param <T> the type of value produced by each promise
   *
   * @return an instance of {@link TracedParallelBatch}.
   */
  public static <T> TracedParallelBatch<T> of(Promise<T>... promises) {
    return new TracedParallelBatch<>(Arrays.asList(promises));
  }

  /**
   * Create a {@link TracedParallelBatch} for list of {@link Promise}s,
   * with the specified trace context.
   *
   * @param context the trace context
   * @param promises an iterable of Promises
   * @param <T> the type of value produced by each promise
   *
   * @return an instance of {@link ParallelBatch}.
   */
  public static <T> ParallelBatch<T> of(final TraceContext context, Iterable<? extends Promise<T>> promises) {
    return ParallelBatch.of(promises)
        .execInit(execution ->
            execution.add(new RatpackCurrentTraceContext.TraceContextHolder(context)));
  }

  /**
   * Create a {@link TracedParallelBatch} for list of {@link Promise}s,
   * with the specified trace context.
   *
   * @param context the trace context.
   * @param promises vararg containing a list of {@link Promise}s.
   * @param <T> the type of value produced by each promise
   *
   * @return an instance of {@link ParallelBatch}.
   */
  public static <T> ParallelBatch<T> of(final TraceContext context, Promise<T>... promises) {
    return of(context, Arrays.asList(promises));
  }

  /**
   * Set the trace context.
   *
   * @param context the trace context.
   * @return a ParallelBatch
   */
  public ParallelBatch<T> withContext(final TraceContext context) {
    return of(context, promises);
  }

}
