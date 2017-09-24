package ratpack.zipkin.support

import zipkin2.Span
import zipkin2.reporter.Reporter

import java.util.concurrent.ConcurrentLinkedDeque

class TestReporter implements Reporter<Span> {

	private final ConcurrentLinkedDeque<Span> spans = new ConcurrentLinkedDeque<>()

	@Override
	void report(Span span) {
		spans.add(span)
	}

	List<Span> getSpans() {
		return spans.asImmutable().toList()
	}

	void reset() {
		spans.clear()
	}
}
