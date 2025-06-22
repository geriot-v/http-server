package ru.netology;

import java.io.BufferedOutputStream;

public class Main {
    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            try {
                String last = request.getQueryParam("last");
                String user = request.getQueryParam("user");

                String html = "<html><body>" +
                        "<h1>Messages</h1>" +
                        "<p>Last: " + last + "</p>" +
                        "<p>User: " + user + "</p>" +
                        "</body></html>";

                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + html.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" + html;

                responseStream.write(response.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.listen(9999);
    }
}