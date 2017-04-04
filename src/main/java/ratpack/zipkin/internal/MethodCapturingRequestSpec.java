package ratpack.zipkin.internal;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.http.HttpMethod;
import ratpack.http.MutableHeaders;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;

/**
 * "Dummy" implementation of RequestSpec, used to capture the HTTP request method.
 *
 */
class MethodCapturingRequestSpec implements RequestSpec {

  private final RequestSpec actualSpec;
  private HttpMethod capturedMethod = null;

  public MethodCapturingRequestSpec(RequestSpec spec) {
    this.actualSpec = spec;
  }

  @Override
  public RequestSpec redirects(int maxRedirects) {
    return this;
  }

  @Override
  public RequestSpec onRedirect(Function<? super ReceivedResponse, Action<? super RequestSpec>> function) {
    return this;
  }

  @Override
  public RequestSpec sslContext(SSLContext sslContext) {
    return this;
  }

  @Override
  public MutableHeaders getHeaders() {
    return new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
  }

  @Override
  public RequestSpec maxContentLength(int numBytes) {
    return this;
  }

  @Override
  public RequestSpec headers(Action<? super MutableHeaders> action) throws Exception {
    return this;
  }

  @Override
  public RequestSpec method(HttpMethod method) {
    capturedMethod = method;
    return this;
  }

  @Override
  public RequestSpec decompressResponse(boolean shouldDecompress) {
    return this;
  }

  @Override
  public URI getUri() {
    return actualSpec.getUri();
  }

  @Override
  public RequestSpec connectTimeout(Duration duration) {
    return this;
  }

  @Override
  public RequestSpec readTimeout(Duration duration) {
    return this;
  }

  @Override
  public Body getBody() {
    return new NoopBodyImpl();
  }

  @Override
  public RequestSpec body(Action<? super Body> action) throws Exception {
    return this;
  }

  public HttpMethod getCapturedMethod() {
    return capturedMethod;
  }

  private class NoopBodyImpl implements Body {
    @Override
    public Body type(String contentType) {
      return this;
    }

    @Override
    public Body stream(Action<? super OutputStream> action) throws Exception {
      return this;
    }

    @Override
    public Body buffer(ByteBuf byteBuf) {
      return this;
    }

    @Override
    public Body bytes(byte[] bytes) {
      return this;
    }

    @Override
    public Body text(CharSequence text) {
      return this;
    }

    @Override
    public Body text(CharSequence text, Charset charset) {
      return this;
    }
  }
}
