package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Server {
    private int port;
    private ExecutorService threadPool;
    private HashMap<String, HashMap<String, Handler>> handlers = new HashMap<>();
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

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

        private Request buildRequest(BufferedReader in) throws IOException {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length == 3) {
                var requestBuilder = Request.newBuilder();
                requestBuilder.setMethod(parts[0]).setPath(parts[1]);

                int bodySize = 0;
                String headerLine = in.readLine();
                while (headerLine.length() > 0) {
                    int headerEnd = headerLine.indexOf(':');
                    String header = headerLine.substring(0, headerEnd);
                    String value = headerLine.substring(headerEnd + 1);
                    if(header.equals("Content-length")){
                        bodySize = Integer.valueOf(value);
                    }
                    requestBuilder.addHeader(header, value);
                    headerLine = in.readLine();
                }

                if(bodySize > 0){
                    StringBuilder bodyBuilder = new StringBuilder();
                    for(int i = 0; i < bodySize; i++){
                        bodyBuilder.append((char) in.read());
                    }

                    requestBuilder.setBody(bodyBuilder.toString());
                }

                return requestBuilder.build();
            }

            return null;
        }

        @Override
        public void run() {
            try (
                    final var in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    final var out = new BufferedOutputStream(client.getOutputStream())
            ) {
                var request = buildRequest(in);

                if(request == null) {
                    return;
                }

                try {
                    var methodHandlers = handlers.get(request.getMethod());
                    methodHandlers.get(request.getPath()).handle(request, out);
                } catch (Exception e){
                    sendError404(out);
                }

                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
