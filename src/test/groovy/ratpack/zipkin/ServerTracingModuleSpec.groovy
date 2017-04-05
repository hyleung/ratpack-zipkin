/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ratpack.zipkin

import brave.Tracer
import brave.sampler.Sampler
import com.github.kristofa.brave.http.SpanNameProvider
import org.assertj.core.util.Lists
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.guice.Guice
import ratpack.server.ServerConfig
import spock.lang.Ignore
import spock.lang.Specification
import zipkin.reporter.Reporter

class ServerTracingModuleSpec extends Specification {


    def 'Should initialize with default config'() {
        given:
            def app = GroovyEmbeddedApp.of { server ->
                server.registry(Guice.registry { binding ->
                    binding.module(ServerTracingModule.class)
                }).handlers { chain -> chain.all { ctx -> ctx.render("foo") } }
            }
        expect:
            app.test { t -> t.get()}
    }

    def 'Should initialize with all the configs!'() {
        given:
            def app = GroovyEmbeddedApp.of { server ->
                server.registry(Guice.registry { binding ->
                    binding.module(ServerTracingModule.class, { config ->
                        config
                            .spanReporter(Stub(Reporter))
                            .spanNameProvider(Stub(SpanNameProvider))
                            .requestAnnotations{request -> Lists.emptyList()}
                            .responseAnnotations(){response -> Lists.emptyList()}
                    })
                }).handlers { chain -> chain.all { ctx -> ctx.render("foo") } }
            }
        expect:
            app.test { t -> t.get()}
    }

    def 'Should initialize with service name config'() {
        given:
        def app = GroovyEmbeddedApp.of { server ->
            server.registry(Guice.registry { binding ->
                binding.module(ServerTracingModule.class, { config ->
                    config.serviceName("embedded")
                })
            }).handlers { chain -> chain.all { ctx -> ctx.render("foo") } }
        }
        expect:
        app.test { t -> t.get()}
    }

    def 'Should collect spans with Reporter'() {
        given:
            def reporter = Mock(Reporter)
            def app = GroovyEmbeddedApp.of { server ->
                server.registry(Guice.registry { binding ->
                    binding.module(ServerTracingModule.class, { config ->
                        config
                                .serviceName("embedded")
                                .spanReporter(reporter)
                    })
                }).handlers { chain -> chain.all { ctx -> ctx.render("foo") } }
            }
        when:
            app.test { t -> t.get()}
        then:
            1 * reporter.report(_)
    }


    def 'Should not collect spans with 0 pct. sampling'() {
        given:
            def spanReporter = Mock(Reporter)
            def app = GroovyEmbeddedApp.of { server ->
                server.registry(Guice.registry { binding ->
                    binding.module(ServerTracingModule.class, { config ->
                        config
                            .serviceName("embedded")
                            .sampler(Sampler.NEVER_SAMPLE)
                            .spanReporter(spanReporter)
                })
            }).handlers { chain -> chain.all { ctx -> ctx.render("foo") } }
        }
        when:
            app.test { t -> t.get()}
        then:
            0 * spanReporter.report(_)
    }

    @Ignore
    def 'ServerConfig ipv4 address Should override brave local address' () {
        given:
        int port = 12345
        byte[] addressBytes = [255, 0, 0, 0]
        InetAddress address = InetAddress.getByAddress(addressBytes)
        int addressAsInt = 255 << 24
        ServerTracingModule.Config config = new ServerTracingModule.Config()
        ServerConfig serverConfig = Mock(ServerConfig)

        when:
        Tracer tracer = new ServerTracingModule().getTracer(config, serverConfig)

        then:
//        tracer.localSpanThreadBinder().state instanceof RatpackServerClientLocalSpanState
//        ((RatpackServerClientLocalSpanState) tracer.localSpanThreadBinder().state).endpoint().ipv4 == addressAsInt
        1 * serverConfig.getPort() >> port
        1 * serverConfig.getAddress() >> address
    }

    @Ignore
    def 'ServerConfig ipv6 address Should override brave local address' () {
        given:
        int port = 12345
        byte[] addressBytes = [255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        InetAddress address = InetAddress.getByAddress(addressBytes)
        ServerTracingModule.Config config = new ServerTracingModule.Config()
        ServerConfig serverConfig = Mock(ServerConfig)

        when:
        Tracer tracer = new ServerTracingModule().getTracer(config, serverConfig)

        then:
//        tracer.localSpanThreadBinder().state instanceof RatpackServerClientLocalSpanState
//        ((RatpackServerClientLocalSpanState) tracer.localSpanThreadBinder().state).endpoint().ipv6 == addressBytes
        1 * serverConfig.getPort() >> port
        1 * serverConfig.getAddress() >> address
    }
}
