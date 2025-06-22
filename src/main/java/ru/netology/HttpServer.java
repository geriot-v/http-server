package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
  private final int port;
  private final List<String> validPaths;
  private final Path publicRoot;
  private ExecutorService executor;

  public HttpServer(int port) {
    this.port = port;
    this.validPaths = List.of(
            "/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js",
            "/links.html", "/forms.html", "/classic.html",
            "/events.html", "/events.js"
    );
    this.publicRoot = Paths.get("public");
    this.executor = Executors.newFixedThreadPool(64);
  }

  public void start() {
    try (final var serverSocket = new ServerSocket(port)) {
      System.out.println("Server started on port " + port);
      while (true) {
        Socket socket = serverSocket.accept();
        executor.submit(() -> handleConnection(socket));
      }
    } catch (IOException e) {
      System.err.println("Error starting server: " + e.getMessage());
      e.printStackTrace();
    } finally {
      shutdown();
    }
  }

  private void handleConnection(Socket socket) {
    try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
    ) {
      String requestLine = in.readLine();
      if (requestLine == null || requestLine.isEmpty()) {
        return;
      }

      String[] parts = requestLine.split(" ");
      if (parts.length != 3) {
        sendBadRequest(out);
        return;
      }

      String path = parts[1];
      if (!validPaths.contains(path)) {
        sendNotFound(out);
        return;
      }

      Path filePath = publicRoot.resolve(path.substring(1));
      if (!Files.exists(filePath)) {
        sendNotFound(out);
        return;
      }

      if ("/classic.html".equals(path)) {
        processClassicHtml(filePath, out);
      } else {
        processStaticFile(filePath, out);
      }

    } catch (IOException e) {
      System.err.println("Error handling connection: " + e.getMessage());
    } finally {
      try {
        socket.close();
      } catch (IOException ignored) {}
    }
  }

  private void sendBadRequest(BufferedOutputStream out) throws IOException {
    String response = "HTTP/1.1 400 Bad Request\r\n" +
            "Content-Length: 0\r\n" +
            "Connection: close\r\n" +
            "\r\n";
    out.write(response.getBytes());
    out.flush();
  }

  private void sendNotFound(BufferedOutputStream out) throws IOException {
    String response = "HTTP/1.1 404 Not Found\r\n" +
            "Content-Length: 0\r\n" +
            "Connection: close\r\n" +
            "\r\n";
    out.write(response.getBytes());
    out.flush();
  }

  private void processClassicHtml(Path filePath, BufferedOutputStream out) throws IOException {
    String template = Files.readString(filePath);
    String content = template.replace("{time}", LocalDateTime.now().toString());
    byte[] bytes = content.getBytes();

    String header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html\r\n" +
            "Content-Length: " + bytes.length + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";

    out.write(header.getBytes());
    out.write(bytes);
    out.flush();
  }

  private void processStaticFile(Path filePath, BufferedOutputStream out) throws IOException {
    String mimeType = Files.probeContentType(filePath);
    long length = Files.size(filePath);

    String header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: " + mimeType + "\r\n" +
            "Content-Length: " + length + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";

    out.write(header.getBytes());
    Files.copy(filePath, out);
    out.flush();
  }

  private void shutdown() {
    System.out.println("Shutting down server...");
    executor.shutdown();
  }

  public static void main(String[] args) {
    HttpServer server = new HttpServer(9999);
    server.start();
  }
}