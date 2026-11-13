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
import kong.unirest.HttpRequest;
import org.apache.hc.client5.http.config.RequestConfig;

import java.util.concurrent.TimeUnit;

class DefaultFactory implements RequestConfigFactory {
    @Override
    public RequestConfig apply(Config config, HttpRequest request) {
        RequestConfig.Builder builder = RequestConfig.custom()
                .setConnectTimeout(request.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .setResponseTimeout(request.getSocketTimeout(), TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(request.getSocketTimeout(), TimeUnit.MILLISECONDS)
                .setProxy(RequestOptions.toApacheProxy(request.getProxy()))
                .setCookieSpec(cookieSpec(config.getCookieSpec()));

        return builder.build();
    }
    private String cookieSpec(String spec) {
        if (spec == null || "standard".equalsIgnoreCase(spec) || "default".equalsIgnoreCase(spec)) {
            return org.apache.hc.client5.http.cookie.StandardCookieSpec.RELAXED;
        }
        if ("ignoreCookies".equalsIgnoreCase(spec)) {
            return org.apache.hc.client5.http.cookie.StandardCookieSpec.IGNORE;
        }
        if ("standard-strict".equalsIgnoreCase(spec)) {
            return org.apache.hc.client5.http.cookie.StandardCookieSpec.STRICT;
        }
        return spec;
    }
}
