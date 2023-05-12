package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Server {

    public static final String GET = "GET";
    public static final String POST = "POST";
    private int port;
    private ExecutorService threadPool;
    private HashMap<String, HashMap<String, Handler>> handlers = new HashMap<>();
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js", "/");

    public void listen(int port) {
        this.port = port;
        try (final var serverSocket = new ServerSocket(port)) {
            threadPool = Executors.newFixedThreadPool(64);
            acceptClients(serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            threadPool.shutdown();
        }
    }

    public void addHandler(String requestType, String path, Handler handler) {
        if (!handlers.containsKey(requestType)) {
            handlers.put(requestType, new HashMap<>());
        }

        HashMap<String, Handler> requestHandlers = handlers.get(requestType);
        requestHandlers.put(path, handler);
    }

    private void acceptClients(ServerSocket serverSocket) throws IOException {
        for (; ; ) {
            threadPool.execute(new ClientThread(serverSocket.accept()));
        }
    }

    private class ClientThread implements Runnable {
        private Socket client;

        ClientThread(Socket client) {
            this.client = client;
        }

        private void sendError404(BufferedOutputStream out) throws IOException {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }

        private static void badRequest(BufferedOutputStream out) throws IOException {
            out.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }

        private static Optional<String> extractHeader(List<String> headers, String header) {
            return headers.stream()
                    .filter(o -> o.startsWith(header))
                    .map(o -> o.substring(o.indexOf(" ")))
                    .map(String::trim)
                    .findFirst();
        }

        // from Google guava with modifications
        private static int indexOf(byte[] array, byte[] target, int start, int max) {
            outer:
            for (int i = start; i < max - target.length + 1; i++) {
                for (int j = 0; j < target.length; j++) {
                    if (array[i + j] != target[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }

        private Request buildRequest(BufferedInputStream in) throws IOException {
            int limit = 4096;
            var reqBuilder = Request.newBuilder();

            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                return null;
            }

            // читаем request line
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                return null;
            }

            reqBuilder.setMethod(requestLine[0]).setPath(requestLine[1]);

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                return null;
            }

            in.reset();
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

            reqBuilder.addHeaders(headers);

            // для GET тела нет
            if (!reqBuilder.getMethod().equals(GET)) {
                in.skip(headersDelimiter.length);
                // вычитываем Content-Length, чтобы прочитать body
                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);

                    final var body = new String(bodyBytes);
                    reqBuilder.setBody(body);
                }
            }

            return reqBuilder.build();
        }

        @Override
        public void run() {
            int limit = 4096;

            try (
                    final var in = new BufferedInputStream(client.getInputStream());
                    final var out = new BufferedOutputStream(client.getOutputStream())
            ) {
                in.mark(limit);
                var request = buildRequest(in);

                if (request == null) {
                    badRequest(out);
                    return;
                }

                try {
                    var methodHandlers = handlers.get(request.getMethod());
                    methodHandlers.get(request.getPath()).handle(request, out);
                } catch (Exception e) {
                    sendError404(out);
                }

                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
