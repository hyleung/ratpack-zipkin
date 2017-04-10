package ratpack.zipkin.support


enum B3PropagationHeaders {
    TRACE_ID("X-B3-TraceId"),
    SPAN_ID("X-B3-SpanId"),
    PARENT_ID("X-B3-ParentSpanId"),
    SAMPLED("X-B3-Sampled")

    public final String value;
    B3PropagationHeaders(final String value) {
        this.value = value
    }
}