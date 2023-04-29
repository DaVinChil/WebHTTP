package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Handler;

public class Server {
    private final int PORT = 9999;
    private ExecutorService threadPool;
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public void launchServer() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            threadPool = Executors.newFixedThreadPool(64);
            acceptClients(serverSocket);
        } catch (IOException e){
            e.printStackTrace();
            threadPool.shutdown();
        }
    }


    private void acceptClients(ServerSocket serverSocket) throws IOException {
        for(;;){
            threadPool.execute(new ClientThread(serverSocket.accept()));
        }
    }

    private class ClientThread implements Runnable {
        private Socket client;

        ClientThread(Socket client){
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

        private void sendClassicPage(BufferedOutputStream out) throws IOException {
            final var filePath = Path.of(".", "public", "/classic.html");
            final var mimeType = Files.probeContentType(filePath);

            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        }

        private void sendPage(BufferedOutputStream out, String path) throws IOException {
            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        }

        @Override
        public void run() {
            try (
                    final var in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    final var out = new BufferedOutputStream(client.getOutputStream())
            ) {
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length == 3) {
                    final var path = parts[1];

                    if (!validPaths.contains(path)) {
                        sendError404(out);
                    } else if (path.equals("/classic.html")) {
                        sendClassicPage(out);
                    } else {
                        sendPage(out, path);
                    }
                }

                client.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
