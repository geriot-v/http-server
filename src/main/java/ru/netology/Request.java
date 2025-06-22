package ru.netology;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.CharArrayBuffer;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, List<String>> queryParams;
    private final Map<String, String> headers;
    private final InputStream body;

    public Request(String method, String uri, Map<String, String> headers, InputStream body) throws URISyntaxException {
        this.method = method;
        this.headers = headers;
        this.body = body;

        // Парсим URI на путь и Query
        URIBuilder uriBuilder = new URIBuilder(new URI(uri));
        this.path = uriBuilder.getPath();
        this.queryParams = new HashMap<>();

        for (var param : uriBuilder.getQueryParams()) {
            queryParams.computeIfAbsent(param.getName(), k -> new ArrayList<>()).add(param.getValue());
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryParam(String name) {
        List<String> values = queryParams.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public List<String> getQueryParams(String name) {
        return queryParams.getOrDefault(name, Collections.emptyList());
    }

    public Map<String, List<String>> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }
}