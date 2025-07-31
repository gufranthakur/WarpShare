package com.warpshare.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
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

        System.out.println("[INFO] HTTP server started on port " + port);
    }

    public class FileUploadHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleFileUpload(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                System.out.println("[ERROR] Method not allowed: " + exchange.getRequestMethod());
            }
        }

        public void handleFileUpload(HttpExchange exchange) throws IOException {
            System.out.println("[INFO] Receiving file upload request");

            InputStream inputStream = exchange.getRequestBody();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            byte[] fullContent = buffer.toByteArray();

            System.out.println("[INFO] Read " + fullContent.length + " bytes from request");

            String boundary = getBoundary(exchange);
            MultipartResult result = parseMultipartContent(fullContent, boundary);

            System.out.println("[INFO] Extracted filename: " + result.filename);
            System.out.println("[INFO] Relative path: " + result.relativePath);
            System.out.println("[INFO] File content size: " + result.content.length + " bytes");

            // Use relative path if provided, otherwise use filename
            String pathToUse = result.relativePath != null ? result.relativePath : result.filename;
            Path filePath = Paths.get(System.getProperty("user.home"), "Downloads", pathToUse);

            // Create parent directories if they don't exist
            try {
                Files.createDirectories(filePath.getParent());
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to create directories: " + e.getMessage());
            }

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(result.content);
                System.out.println("[INFO] File saved to: " + filePath);
            }

            Platform.runLater(() -> {
                receivedFiles.add(pathToUse);
                progressCallback.accept(1.0);
            });

            String response = "File uploaded successfully";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }

            System.out.println("[INFO] Upload completed successfully");
        }

        private String getBoundary(HttpExchange exchange) {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null && contentType.contains("boundary=")) {
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
                System.out.println("[INFO] Found boundary: " + boundary);
                return boundary;
            }
            System.out.println("[ERROR] No boundary found in Content-Type");
            return null;
        }

        private class MultipartResult {
            String filename;
            String relativePath;
            byte[] content;

            MultipartResult(String filename, String relativePath, byte[] content) {
                this.filename = filename;
                this.relativePath = relativePath;
                this.content = content;
            }
        }

        private MultipartResult parseMultipartContent(byte[] data, String boundary) {
            if (boundary == null) {
                System.out.println("[ERROR] No boundary provided, using raw data");
                return new MultipartResult("received_file_" + System.currentTimeMillis(), null, data);
            }

            String dataStr = new String(data);
            String boundaryStr = "--" + boundary;

            String[] parts = dataStr.split(boundaryStr);
            String fileName = "received_file_" + System.currentTimeMillis();
            String relativePath = null;
            byte[] fileContent = null;

            for (String part : parts) {
                if (part.trim().isEmpty() || part.startsWith("--")) continue;

                int headerEnd = part.indexOf("\r\n\r\n");
                if (headerEnd == -1) continue;

                String headers = part.substring(0, headerEnd);
                String content = part.substring(headerEnd + 4);

                // Remove trailing boundary markers
                if (content.endsWith("\r\n")) {
                    content = content.substring(0, content.length() - 2);
                }

                if (headers.contains("name=\"file\"")) {
                    // This is the file content
                    if (headers.contains("filename=")) {
                        int fnStart = headers.indexOf("filename=\"") + 10;
                        int fnEnd = headers.indexOf("\"", fnStart);
                        if (fnStart < fnEnd && fnStart > 9) {
                            fileName = headers.substring(fnStart, fnEnd);
                            System.out.println("[INFO] Parsed filename from headers: " + fileName);
                        }
                    }
                    try {
                        fileContent = content.getBytes("ISO-8859-1"); // Preserve binary data
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                } else if (headers.contains("name=\"relativePath\"")) {
                    // This is the relative path
                    relativePath = content.trim();
                    System.out.println("[INFO] Parsed relative path: " + relativePath);
                }
            }

            if (fileContent == null) {
                System.out.println("[ERROR] No file content found, using raw data");
                fileContent = data;
            }

            System.out.println("[INFO] Multipart parsing complete - filename: " + fileName +
                    ", relativePath: " + relativePath + ", content size: " + fileContent.length);
            return new MultipartResult(fileName, relativePath, fileContent);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[INFO] HTTP server stopped");
        }
    }
}