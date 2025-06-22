package ru.netology;


import ru.netology.Server;

import java.io.BufferedOutputStream;

public class Main {
    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            try {
                String html = "<html><body><h1>GET /messages</h1></body></html>";
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + html.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" + html;
                responseStream.write(response.getBytes());
                responseStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> {
            try {
                String text = "POST to /messages received!";
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + text.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" + text;
                responseStream.write(response.getBytes());
                responseStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.listen(9999);
    }
}