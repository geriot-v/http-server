package ru.netology;

import java.io.BufferedOutputStream;
import java.io.InputStream;

@FunctionalInterface
public interface Handler {
    void handle(Request request, BufferedOutputStream responseStream);
}