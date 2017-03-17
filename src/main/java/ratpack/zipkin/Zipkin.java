package ratpack.zipkin;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binding annotation used for injecting Zipkin-instrumented things.
 *
 * Currently, only used to inject Zipkin-instrumented {@link ratpack.http.client.HttpClient}
 * instances.
 */
@BindingAnnotation @Target({ElementType.PARAMETER, ElementType.FIELD}) @Retention(RetentionPolicy.RUNTIME)
public @interface Zipkin {}
