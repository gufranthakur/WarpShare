package com.warpshare.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class HttpServerService {
    public HttpServer server; // Changed from HttpsServer to HttpServer
    public ObservableList<String> receivedFiles;
    public Consumer<Double> progressCallback;

    public void startServer(int port, ObservableList<String> receivedFiles, Consumer<Double> progressCallback) throws Exception {
        this.receivedFiles = receivedFiles;
        this.progressCallback = progressCallback;

        // Changed from HttpsServer to HttpServer
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Removed SSL configuration entirely
        server.createContext("/upload", new FileUploadHandler());
        server.setExecutor(null);
        server.start();
    }

    // Removed createSSLContext method entirely

    public class FileUploadHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleFileUpload(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        public void handleFileUpload(HttpExchange exchange) throws IOException {
            InputStream inputStream = exchange.getRequestBody();

            String fileName = "received_file_" + System.currentTimeMillis();
            Path filePath = Paths.get(System.getProperty("user.home"), "Downloads", fileName);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;

                    double progress = Math.min(1.0, totalBytes / 1000000.0);
                    Platform.runLater(() -> progressCallback.accept(progress));
                }
            }

            Platform.runLater(() -> receivedFiles.add(fileName));

            String response = "File uploaded successfully";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}