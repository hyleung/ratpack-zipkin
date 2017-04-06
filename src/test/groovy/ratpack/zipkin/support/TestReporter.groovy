package ratpack.zipkin.support

import zipkin.Span
import zipkin.reporter.Reporter


class TestReporter implements Reporter<Span> {

	private final List<Span> spans = new LinkedList<>()

	@Override
	void report(Span span) {
		spans.add(span)
	}

	List<Span> getSpans() {
		return spans
	}

	void reset() {
		spans.clear()
	}
}
