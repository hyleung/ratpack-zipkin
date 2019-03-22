package ratpack.zipkin.internal;

import brave.http.HttpClientAdapter;
import ratpack.http.client.RequestSpec;

public class ClientHttpAdapter extends HttpClientAdapter<RequestSpec, Integer> {

    @Override
    public String method(RequestSpec request) {
        return request.getMethod().getName();
    }

    @Override
    public String url(RequestSpec request) {
        return request.getUri().toString();
    }

    @Override
    public String path(RequestSpec request) {
        return request.getUri().getPath();
    }

    @Override
    public String requestHeader(RequestSpec request, String name) {
        return request.getHeaders().get(name);
    }

    @Override
    public Integer statusCode(Integer status) {
        return status;
    }
}
