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

import com.github.kristofa.brave.Brave
import com.github.kristofa.brave.Sampler
import com.github.kristofa.brave.SpanCollector
import com.github.kristofa.brave.http.SpanNameProvider
import com.twitter.zipkin.gen.Span
import org.assertj.core.util.Lists
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.guice.Guice
import ratpack.server.ServerConfig
import ratpack.zipkin.internal.RatpackServerClientLocalSpanState
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
                            .sampler(Sampler.create(1f))
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

    /**
     * SpanCollector is deprecated.
     *
     * Retaining this test until ServerTracingModule.Config.spanCollector(SpanCollector)
     * is removed.
     */
    def 'Should collect spans with SpanCollector'() {
        given:
            def spanCollector = Mock(SpanCollector)
            def app = GroovyEmbeddedApp.of { server ->
                server.registry(Guice.registry { binding ->
                    binding.module(ServerTracingModule.class, { config ->
                        config
                            .serviceName("embedded")
                            .spanCollector(spanCollector)
                    })
                }).handlers { chain -> chain.all { ctx -> ctx.render("foo") } }
            }
        when:
            app.test { t -> t.get()}
        then:
            1 * spanCollector.collect(_ as Span)
    }

    def 'Should not collect spans with 0 pct. sampling'() {
        given:
            def spanReporter = Mock(Reporter)
            def app = GroovyEmbeddedApp.of { server ->
                server.registry(Guice.registry { binding ->
                    binding.module(ServerTracingModule.class, { config ->
                        config
                            .serviceName("embedded")
                            .spanReporter(spanReporter)
                            .sampler(Sampler.create(0))
                })
            }).handlers { chain -> chain.all { ctx -> ctx.render("foo") } }
        }
        when:
            app.test { t -> t.get()}
        then:
            0 * spanReporter.report(_)
    }

    def 'ServerConfig ipv4 address Should override brave local address' () {
        given:
        int port = 12345
        byte[] addressBytes = [255, 0, 0, 0]
        InetAddress address = InetAddress.getByAddress(addressBytes)
        int addressAsInt = 255 << 24
        ServerTracingModule.Config config = new ServerTracingModule.Config()
        ServerConfig serverConfig = Mock(ServerConfig)

        when:
        Brave brave = new ServerTracingModule().getBrave(config, serverConfig)

        then:
        brave.localSpanThreadBinder().state instanceof RatpackServerClientLocalSpanState
        ((RatpackServerClientLocalSpanState) brave.localSpanThreadBinder().state).endpoint().ipv4 == addressAsInt
        1 * serverConfig.getPort() >> port
        1 * serverConfig.getAddress() >> address
    }

    def 'ServerConfig ipv6 address Should override brave local address' () {
        given:
        int port = 12345
        byte[] addressBytes = [255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        InetAddress address = InetAddress.getByAddress(addressBytes)
        ServerTracingModule.Config config = new ServerTracingModule.Config()
        ServerConfig serverConfig = Mock(ServerConfig)

        when:
        Brave brave = new ServerTracingModule().getBrave(config, serverConfig)

        then:
        brave.localSpanThreadBinder().state instanceof RatpackServerClientLocalSpanState
        ((RatpackServerClientLocalSpanState) brave.localSpanThreadBinder().state).endpoint().ipv6 == addressBytes
        1 * serverConfig.getPort() >> port
        1 * serverConfig.getAddress() >> address
    }
}
