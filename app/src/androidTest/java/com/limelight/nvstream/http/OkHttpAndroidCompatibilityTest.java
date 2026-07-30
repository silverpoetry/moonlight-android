package com.limelight.nvstream.http;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.EventListener;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Timeout;
import kotlin.jvm.functions.Function0;
import kotlin.reflect.KClass;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class OkHttpAndroidCompatibilityTest {
    @Test
    public void synchronousPostPreservesPathHeadersBodyAndResponse() throws Exception {
        byte[] requestBody = "manifest-payload".getBytes(StandardCharsets.UTF_8);
        try (LoopbackHttpServer server =
                     new LoopbackHttpServer("accepted", 0)) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .writeTimeout(2, TimeUnit.SECONDS)
                    .build();
            try {
                Request request = new Request.Builder()
                        .url(server.url("/api/files/desktop?offset=7"))
                        .header("X-Moonlight-Transfer-Token", "test-token")
                        .post(RequestBody.create(
                                requestBody,
                                MediaType.parse("application/octet-stream")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    assertTrue(response.isSuccessful());
                    assertNotNull(response.body());
                    assertEquals("accepted", response.body().string());
                }

                assertTrue(server.awaitRequest());
                assertEquals("POST /api/files/desktop?offset=7 HTTP/1.1",
                        server.requestLine);
                assertEquals("test-token",
                        server.headerValue("X-Moonlight-Transfer-Token"));
                assertEquals("application/octet-stream",
                        server.headerValue("Content-Type"));
                assertArrayEquals(requestBody, server.requestBody);
            }
            finally {
                closeClient(client);
            }
        }
    }

    @Test
    public void asynchronousCallbackReceivesAndClosesResponse() throws Exception {
        try (LoopbackHttpServer server =
                     new LoopbackHttpServer("update-metadata", 0)) {
            OkHttpClient client = new OkHttpClient();
            try {
                Request request = new Request.Builder()
                        .url(server.url("/updates"))
                        .get()
                        .build();
                CountDownLatch completed = new CountDownLatch(1);
                AtomicReference<String> body = new AtomicReference<>();
                AtomicReference<Throwable> failure = new AtomicReference<>();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException error) {
                        failure.set(error);
                        completed.countDown();
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (Response closeableResponse = response) {
                            if (response.body() == null) {
                                throw new IOException(
                                        "Missing response body");
                            }
                            body.set(response.body().string());
                        }
                        catch (Throwable error) {
                            failure.set(error);
                        }
                        finally {
                            completed.countDown();
                        }
                    }
                });

                assertTrue(completed.await(3, TimeUnit.SECONDS));
                if (failure.get() != null) {
                    throw new AssertionError(failure.get());
                }
                assertEquals("update-metadata", body.get());
            }
            finally {
                closeClient(client);
            }
        }
    }

    @Test
    public void readTimeoutAndCancellationRemainPrompt() throws Exception {
        try (LoopbackHttpServer timeoutServer =
                     new LoopbackHttpServer("late", 1_000)) {
            OkHttpClient timeoutClient = new OkHttpClient.Builder()
                    .readTimeout(100, TimeUnit.MILLISECONDS)
                    .build();
            try {
                Request timeoutRequest = new Request.Builder()
                        .url(timeoutServer.url("/timeout"))
                        .build();
                long startedAt = System.nanoTime();
                try (Response ignored =
                             timeoutClient.newCall(timeoutRequest).execute()) {
                    fail("Expected read timeout");
                }
                catch (SocketTimeoutException expected) {
                    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt);
                    assertTrue("Timeout was delayed: " + elapsedMillis,
                            elapsedMillis < 1_000);
                }
            }
            finally {
                closeClient(timeoutClient);
            }
        }

        try (LoopbackHttpServer cancellationServer =
                     new LoopbackHttpServer("late", 1_000)) {
            OkHttpClient client = new OkHttpClient();
            try {
                Request request = new Request.Builder()
                        .url(cancellationServer.url("/cancel"))
                        .build();
                CountDownLatch completed = new CountDownLatch(1);
                AtomicReference<Throwable> callbackFailure =
                        new AtomicReference<>();
                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call failedCall, IOException error) {
                        callbackFailure.set(error);
                        completed.countDown();
                    }

                    @Override
                    public void onResponse(
                            Call completedCall, Response response) {
                        response.close();
                        completed.countDown();
                    }
                });

                assertTrue(cancellationServer.awaitRequest());
                call.cancel();
                assertTrue(completed.await(2, TimeUnit.SECONDS));
                assertTrue(call.isCanceled());
                assertNotNull(callbackFailure.get());
            }
            finally {
                closeClient(client);
            }
        }
    }

    @Test
    public void synchronousInterruptionIsNormalizedAtJavaBoundary()
            throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();

        Thread worker = new Thread(() -> {
            try {
                OkHttpCalls.execute(new InterruptingCall());
                failure.set(new AssertionError(
                        "Expected interrupted HTTP call to fail"));
            }
            catch (Throwable error) {
                failure.set(error);
                interruptRestored.set(
                        Thread.currentThread().isInterrupted());
            }
        }, "OkHttp interruption compatibility test");

        worker.start();
        worker.join(2_000);

        assertTrue("Worker did not stop", !worker.isAlive());
        assertTrue(failure.get() instanceof InterruptedIOException);
        assertTrue(failure.get().getCause() instanceof InterruptedException);
        assertTrue("Interrupt status was not restored",
                interruptRestored.get());
    }

    private static void closeClient(OkHttpClient client) {
        client.dispatcher().executorService().shutdownNow();
        client.connectionPool().evictAll();
    }

    private static final class LoopbackHttpServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final Thread thread;
        private final byte[] responseBody;
        private final long responseDelayMillis;
        private final CountDownLatch requestReceived = new CountDownLatch(1);

        private volatile boolean closing;
        private volatile Throwable failure;
        private volatile String requestLine;
        private volatile String headers;
        private volatile byte[] requestBody = new byte[0];
        private volatile Socket acceptedSocket;

        LoopbackHttpServer(String responseBody, long responseDelayMillis)
                throws IOException {
            this.responseBody =
                    responseBody.getBytes(StandardCharsets.UTF_8);
            this.responseDelayMillis = responseDelayMillis;
            serverSocket = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
            thread = new Thread(this::serveSingleRequest,
                    "OkHttpAndroidCompatibilityTest");
            thread.start();
        }

        String url(String path) {
            return "http://127.0.0.1:" + serverSocket.getLocalPort() + path;
        }

        boolean awaitRequest() throws InterruptedException {
            return requestReceived.await(2, TimeUnit.SECONDS);
        }

        String headerValue(String name) {
            String prefix = name.toLowerCase(Locale.US) + ":";
            for (String line : headers.split("\r\n")) {
                if (line.toLowerCase(Locale.US).startsWith(prefix)) {
                    return line.substring(line.indexOf(':') + 1).trim();
                }
            }
            return null;
        }

        private void serveSingleRequest() {
            try (Socket socket = serverSocket.accept()) {
                acceptedSocket = socket;
                socket.setSoTimeout(2_000);
                BufferedInputStream input =
                        new BufferedInputStream(socket.getInputStream());
                requestLine = readAsciiLine(input);
                StringBuilder headerBlock = new StringBuilder();
                int contentLength = 0;
                String line;
                while (!(line = readAsciiLine(input)).isEmpty()) {
                    headerBlock.append(line).append("\r\n");
                    if (line.toLowerCase(Locale.US)
                            .startsWith("content-length:")) {
                        contentLength = Integer.parseInt(
                                line.substring(line.indexOf(':') + 1).trim());
                    }
                }
                headers = headerBlock.toString();
                requestBody = readExactly(input, contentLength);
                requestReceived.countDown();

                if (responseDelayMillis > 0) {
                    Thread.sleep(responseDelayMillis);
                }
                OutputStream output = socket.getOutputStream();
                String responseHeaders =
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain; charset=utf-8\r\n" +
                        "Content-Length: " + responseBody.length + "\r\n" +
                        "Connection: close\r\n\r\n";
                output.write(responseHeaders.getBytes(StandardCharsets.US_ASCII));
                output.write(responseBody);
                output.flush();
            }
            catch (Throwable error) {
                if (!closing) {
                    failure = error;
                }
                requestReceived.countDown();
            }
        }

        @Override
        public void close() throws Exception {
            closing = true;
            serverSocket.close();
            Socket socket = acceptedSocket;
            if (socket != null) {
                socket.close();
            }
            thread.join(2_000);
            if (thread.isAlive()) {
                throw new AssertionError("Loopback server did not stop");
            }
            if (failure != null) {
                throw new AssertionError(failure);
            }
        }

        private static String readAsciiLine(InputStream input)
                throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int previous = -1;
            int value;
            while ((value = input.read()) != -1) {
                if (previous == '\r' && value == '\n') {
                    byte[] bytes = output.toByteArray();
                    return new String(
                            bytes, 0, Math.max(0, bytes.length - 1),
                            StandardCharsets.US_ASCII);
                }
                output.write(value);
                previous = value;
            }
            throw new IOException("Unexpected end of HTTP headers");
        }

        private static byte[] readExactly(InputStream input, int length)
                throws IOException {
            byte[] data = new byte[length];
            int offset = 0;
            while (offset < data.length) {
                int read = input.read(data, offset, data.length - offset);
                if (read < 0) {
                    throw new IOException("Unexpected end of request body");
                }
                offset += read;
            }
            return data;
        }
    }

    private static final class InterruptingCall implements Call {
        private final Request request = new Request.Builder()
                .url("http://127.0.0.1/")
                .build();

        @Override
        public Request request() {
            return request;
        }

        @Override
        public Response execute() throws IOException {
            throwUnchecked(new InterruptedException(
                    "Synthetic OkHttp internal wait interruption"));
            throw new AssertionError("Unreachable");
        }

        @Override
        public void enqueue(Callback responseCallback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public Timeout timeout() {
            return new Timeout();
        }

        @Override
        public void addEventListener(EventListener eventListener) {
        }

        @Override
        public <T> T tag(KClass<T> type) {
            return null;
        }

        @Override
        public <T> T tag(Class<? extends T> type) {
            return null;
        }

        @Override
        public <T> T tag(
                KClass<T> type, Function0<? extends T> computeIfAbsent) {
            return computeIfAbsent.invoke();
        }

        @Override
        public <T> T tag(
                Class<T> type, Function0<? extends T> computeIfAbsent) {
            return computeIfAbsent.invoke();
        }

        @Override
        public Call clone() {
            return new InterruptingCall();
        }
    }

    @SuppressWarnings("unchecked")
    private static <Failure extends Throwable> void throwUnchecked(
            Throwable error) throws Failure {
        throw (Failure) error;
    }
}
