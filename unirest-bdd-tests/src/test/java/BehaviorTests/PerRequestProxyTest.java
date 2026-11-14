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

package BehaviorTests;

import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerRequestProxyTest {
    @AfterEach
    void shutdown() {
        Unirest.shutDown(true);
    }

    @Test
    void routesSynchronousRequestThroughItsProxy() throws Exception {
        verifyProxy(false);
    }

    @Test
    void routesAsynchronousRequestThroughItsProxy() throws Exception {
        verifyProxy(true);
    }

    private void verifyProxy(boolean async) throws Exception {
        try (ServerSocket proxy = new ServerSocket(0)) {
            proxy.setSoTimeout(5000);
            CompletableFuture<String> received = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = proxy.accept()) {
                    socket.setSoTimeout(5000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    String requestLine = reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nOK".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    return requestLine;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            String url = "http://example.invalid/proxy-check";
            String body = async
                    ? Unirest.get(url).proxy("localhost", proxy.getLocalPort()).asStringAsync().get(10, TimeUnit.SECONDS).getBody()
                    : Unirest.get(url).proxy("localhost", proxy.getLocalPort()).asString().getBody();
            assertEquals("OK", body);
            assertTrue(received.get(10, TimeUnit.SECONDS).contains(url));
        }
    }
}
