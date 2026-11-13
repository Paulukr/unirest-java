/**
 * The MIT License
 *
 * Copyright for portions of unirest-java are held by Kong Inc (c) 2013.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kong.unirest.apache;

import kong.unirest.Config;
import kong.unirest.UnirestConfigException;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;

import java.util.stream.Stream;

import static kong.unirest.apache.BaseApacheClient.toApacheCreds;

class ApacheAsyncConfig {
    final HttpAsyncClient client;
    final Config config;
    private boolean hookset;
    private org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager manager;
    private AsyncIdleConnectionMonitorThread monitor;

    public ApacheAsyncConfig(Config config) {
        this.config = config;
        try {
            PoolingAsyncClientConnectionManagerBuilder pool = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(config.getMaxConnections())
                    .setMaxConnPerRoute(config.getMaxPerRoutes());
            if (config.getTTL() > 0) {
                pool.setConnectionTimeToLive(TimeValue.ofMilliseconds(config.getTTL()));
            }
            if (!config.isVerifySsl()) {
                pool.setTlsStrategy(ClientTlsStrategyBuilder.create()
                        .setSslContext(new SSLContextBuilder().loadTrustMaterial(null, (chain, auth) -> true).build())
                        .setHostVerificationPolicy(org.apache.hc.client5.http.ssl.HostnameVerificationPolicy.CLIENT)
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE).buildAsync());
            }
            HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create()
                    .setDefaultRequestConfig(RequestOptions.toRequestConfig(config))
                    .setConnectionManager(pool.build())
                    .setDefaultCredentialsProvider(toApacheCreds(config.getProxy()))
                    .evictIdleConnections(TimeValue.ofSeconds(30));
            configureOptions(builder);
            CloseableHttpAsyncClient built = builder.build();
            built.start();
            client = built;
            if (config.shouldAddShutdownHook()) {
                registerShutdownHook();
            }
        } catch (Exception e) {
            throw new UnirestConfigException(e);
        }
    }

    private void configureOptions(HttpAsyncClientBuilder builder) {
        if (config.useSystemProperties()) {
            builder.useSystemProperties();
        }
        if (!config.getFollowRedirects()) {
            builder.disableRedirectHandling();
        }
        if (!config.getEnabledCookieManagement()) {
            builder.disableCookieManagement();
        }
        if (!config.isAutomaticRetries()) {
            builder.disableAutomaticRetries();
        } else {
            builder.setRetryStrategy(new NoResponseRetryStrategy());
        }
        if (!config.isRequestCompressionOn()) {
            builder.disableContentCompression();
        }
        if (config.getEnabledCookieManagement()) {
            builder.addRequestInterceptorFirst(CookieHeaderCompatibility.before());
            builder.addRequestInterceptorLast(CookieHeaderCompatibility.after());
        }
        config.getInterceptor().forEach(builder::addRequestInterceptorFirst);
    }

    public ApacheAsyncConfig(HttpAsyncClient client, Config config) {
        this.client = client;
        this.config = config;
    }

    public ApacheAsyncConfig(HttpAsyncClient client, Config config,
                             org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager manager,
                             AsyncIdleConnectionMonitorThread monitor) {
        this(client, config);
        this.manager = manager;
        this.monitor = monitor;
    }

    public void registerShutdownHook() {
        if (!hookset) {
            hookset = true;
            Runtime.getRuntime().addShutdownHook(new Thread(this::close, "Unirest Apache Async Client Shutdown Hook"));
        }
    }

    public boolean isRunning() {
        return Util.tryCast(client, CloseableHttpAsyncClient.class)
                .map(c -> c.getStatus() == IOReactorStatus.ACTIVE || c.getStatus() == IOReactorStatus.INACTIVE).orElse(true);
    }

    public HttpAsyncClient getClient() { return client; }

    public Stream<Exception> close() {
        return Util.collectExceptions(Util.tryCast(client, CloseableHttpAsyncClient.class)
                        .filter(c -> c.getStatus() == IOReactorStatus.ACTIVE || c.getStatus() == IOReactorStatus.INACTIVE)
                        .map(c -> Util.tryDo(c, CloseableHttpAsyncClient::close))
                        .filter(java.util.Optional::isPresent).map(java.util.Optional::get),
                Util.tryDo(manager, m -> m.close()),
                Util.tryDo(monitor, m -> m.interrupt()));
    }
}
