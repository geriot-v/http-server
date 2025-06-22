package ru.netology;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;

public class Server {
    private final ExecutorService executor = Executors.newFixedThreadPool(64);
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();
    private final Path publicRoot = Paths.get("public");
    private final List<String> validPaths = List.of(
            "/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js",
            "/links.html", "/forms.html", "/classic.html",
            "/events.html", "/events.js"
    );

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>()).put(path, handler);
    }

    public void listen(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            System.err.println("Ошибка при запуске сервера: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isBlank()) {
                sendBadRequest(out);
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                sendBadRequest(out);
                return;
            }

            String method = parts[0];
            String path = parts[1];

            Map<String, String> headers = new HashMap<>();
            String line;
            while (!(line = in.readLine()).isBlank()) {
                String[] headerParts = line.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            int contentLength = 0;
            if (headers.containsKey("Content-Length")) {
                try {
                    contentLength = Integer.parseInt(headers.get("Content-Length"));
                } catch (NumberFormatException ignored) {
                    sendBadRequest(out);
                    return;
                }
            }

            InputStream bodyStream;
            if (contentLength > 0) {
                bodyStream = new LimitInputStream(socket.getInputStream(), contentLength);
            } else {
                bodyStream = new ByteArrayInputStream(new byte[0]);
            }

            Handler handler = Optional.ofNullable(handlers.get(method))
                    .map(map -> map.get(path))
                    .orElse(null);

            if (handler != null) {
                Request request = new Request(method, path, headers, bodyStream);
                handler.handle(request, out);
            } else {
                serveStaticFile(path, out);
            }

        } catch (IOException e) {
            System.err.println("Ошибка при обработке подключения: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void serveStaticFile(String path, BufferedOutputStream out) throws IOException {
        if (!validPaths.contains(path)) {
            sendNotFound(out);
            return;
        }

        Path filePath = publicRoot.resolve(path.startsWith("/") ? path.substring(1) : path);

        if (!Files.exists(filePath)) {
            sendNotFound(out);
            return;
        }

        if ("/classic.html".equals(path)) {
            String template = Files.readString(filePath);
            String content = template.replace("{time}", LocalDateTime.now().toString());
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            String mimeType = Files.probeContentType(filePath);

            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + bytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.flush();
        } else {
            String mimeType = Files.probeContentType(filePath);
            long length = Files.size(filePath);

            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(header.getBytes(StandardCharsets.UTF_8));
            Files.copy(filePath, out);
            out.flush();
        }
    }

    private void sendResponse(BufferedOutputStream out, String statusLine, String contentType, int contentLength) throws IOException {
        String response = statusLine + "\r\n" +
                (contentType != null ? "Content-Type: " + contentType + "\r\n" : "") +
                "Content-Length: " + contentLength + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void sendBadRequest(BufferedOutputStream out) throws IOException {
        sendResponse(out, "HTTP/1.1 400 Bad Request", null, 0);
    }

    private void sendNotFound(BufferedOutputStream out) throws IOException {
        sendResponse(out, "HTTP/1.1 404 Not Found", null, 0);
    }

    private void shutdown() {
        System.out.println("Сервер останавливается.");
        executor.shutdown();
    }

    static class LimitInputStream extends InputStream {
        private final InputStream source;
        private int remaining;

        public LimitInputStream(InputStream source, int limit) {
            this.source = source;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = source.read();
            if (b != -1) remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = Math.min(len, remaining);
            int bytesRead = source.read(b, off, toRead);
            if (bytesRead != -1) remaining -= bytesRead;
            return bytesRead;
        }
    }
}