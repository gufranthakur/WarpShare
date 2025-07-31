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
                if (progressCallback != null) {
                    progressCallback.accept(1.0);
                }
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

            byte[] boundaryBytes = ("--" + boundary).getBytes();
            String fileName = "received_file_" + System.currentTimeMillis();
            String relativePath = null;
            byte[] fileContent = null;

            int pos = 0;
            while (pos < data.length) {
                // Find next boundary
                int boundaryStart = findBytes(data, boundaryBytes, pos);
                if (boundaryStart == -1) break;

                // Find end of this part
                int nextBoundaryStart = findBytes(data, boundaryBytes, boundaryStart + boundaryBytes.length);
                if (nextBoundaryStart == -1) nextBoundaryStart = data.length;

                // Extract headers (text portion)
                int headerStart = boundaryStart + boundaryBytes.length;
                while (headerStart < data.length && (data[headerStart] == '\r' || data[headerStart] == '\n')) {
                    headerStart++;
                }

                int headerEnd = findBytes(data, "\r\n\r\n".getBytes(), headerStart);
                if (headerEnd == -1) {
                    pos = nextBoundaryStart;
                    continue;
                }

                String headers = new String(data, headerStart, headerEnd - headerStart);
                int contentStart = headerEnd + 4;
                int contentEnd = nextBoundaryStart;

                // Remove trailing CRLF before boundary
                while (contentEnd > contentStart && (data[contentEnd - 1] == '\r' || data[contentEnd - 1] == '\n')) {
                    contentEnd--;
                }

                if (headers.contains("name=\"file\"")) {
                    // Extract filename
                    if (headers.contains("filename=")) {
                        int fnStart = headers.indexOf("filename=\"") + 10;
                        int fnEnd = headers.indexOf("\"", fnStart);
                        if (fnStart < fnEnd && fnStart > 9) {
                            fileName = headers.substring(fnStart, fnEnd);
                            System.out.println("[INFO] Parsed filename: " + fileName);
                        }
                    }
                    // Extract binary content
                    fileContent = new byte[contentEnd - contentStart];
                    System.arraycopy(data, contentStart, fileContent, 0, contentEnd - contentStart);
                } else if (headers.contains("name=\"relativePath\"")) {
                    // Extract relative path (text content)
                    relativePath = new String(data, contentStart, contentEnd - contentStart).trim();
                    System.out.println("[INFO] Parsed relative path: " + relativePath);
                }

                pos = nextBoundaryStart;
            }

            if (fileContent == null) {
                System.out.println("[ERROR] No file content found, using raw data");
                fileContent = data;
            }

            System.out.println("[INFO] Parsing complete - filename: " + fileName +
                    ", relativePath: " + relativePath + ", content size: " + fileContent.length);
            return new MultipartResult(fileName, relativePath, fileContent);
        }

        private int findBytes(byte[] data, byte[] pattern, int start) {
            for (int i = start; i <= data.length - pattern.length; i++) {
                boolean found = true;
                for (int j = 0; j < pattern.length; j++) {
                    if (data[i + j] != pattern[j]) {
                        found = false;
                        break;
                    }
                }
                if (found) return i;
            }
            return -1;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[INFO] HTTP server stopped");
        }
    }
}