package com.warpshare.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class HttpServerService {
    public HttpServer server;
    public ObservableList<String> receivedFiles;
    public Consumer<Double> progressCallback;

    public void startServer(int port, ObservableList<String> receivedFiles, Consumer<Double> progressCallback) throws Exception {
        this.receivedFiles = receivedFiles;
        this.progressCallback = progressCallback;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/upload", new FileUploadHandler());
        server.setExecutor(null);
        server.start();

        showInfo("HTTP server started on port " + port);
    }

    public class FileUploadHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleFileUpload(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                showError("Method not allowed: " + exchange.getRequestMethod());
            }
        }

        public void handleFileUpload(HttpExchange exchange) throws IOException {
            showInfo("Receiving file upload request");

            InputStream inputStream = exchange.getRequestBody();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            byte[] fullContent = buffer.toByteArray();

            showInfo("Read " + fullContent.length + " bytes from request");

            String boundary = getBoundary(exchange);
            MultipartResult result = parseMultipartContent(fullContent, boundary);

            showInfo("Extracted filename: " + result.filename);
            showInfo("File content size: " + result.content.length + " bytes");

            Path filePath = Paths.get(System.getProperty("user.home"), "Downloads", result.filename);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(result.content);
                showInfo("File saved to: " + filePath);
            }

            Platform.runLater(() -> {
                receivedFiles.add(result.filename);
                progressCallback.accept(1.0);
            });

            String response = "File uploaded successfully";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }

            showInfo("Upload completed successfully");
        }

        private String getBoundary(HttpExchange exchange) {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null && contentType.contains("boundary=")) {
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
                showInfo("Found boundary: " + boundary);
                return boundary;
            }
            showError("No boundary found in Content-Type");
            return null;
        }

        private class MultipartResult {
            String filename;
            byte[] content;

            MultipartResult(String filename, byte[] content) {
                this.filename = filename;
                this.content = content;
            }
        }

        private MultipartResult parseMultipartContent(byte[] data, String boundary) {
            if (boundary == null) {
                showError("No boundary provided, using raw data");
                return new MultipartResult("received_file_" + System.currentTimeMillis(), data);
            }

            String dataStr = new String(data);
            String boundaryStr = "--" + boundary;

            int start = dataStr.indexOf(boundaryStr);
            if (start == -1) {
                showError("Boundary not found in data, using raw data");
                return new MultipartResult("received_file_" + System.currentTimeMillis(), data);
            }

            int headerEnd = dataStr.indexOf("\r\n\r\n", start);
            if (headerEnd == -1) {
                showError("Header end not found, using raw data");
                return new MultipartResult("received_file_" + System.currentTimeMillis(), data);
            }

            String headers = dataStr.substring(start, headerEnd);
            String fileName = "received_file_" + System.currentTimeMillis();

            if (headers.contains("filename=")) {
                int fnStart = headers.indexOf("filename=\"") + 10;
                int fnEnd = headers.indexOf("\"", fnStart);
                if (fnStart < fnEnd && fnStart > 9) {
                    fileName = headers.substring(fnStart, fnEnd);
                    showInfo("Parsed filename from headers: " + fileName);
                }
            }

            int contentStart = headerEnd + 4;
            int contentEnd = dataStr.lastIndexOf("\r\n--" + boundary);
            if (contentEnd == -1) contentEnd = data.length;

            byte[] fileContent = new byte[contentEnd - contentStart];
            System.arraycopy(data, contentStart, fileContent, 0, contentEnd - contentStart);

            showInfo("Multipart parsing complete - filename: " + fileName + ", content size: " + fileContent.length);
            return new MultipartResult(fileName, fileContent);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            showInfo("HTTP server stopped");
        }
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("WarpShare Info");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("WarpShare Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }
}
