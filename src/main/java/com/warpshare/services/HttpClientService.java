package com.warpshare.services;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class HttpClientService {

    public void sendFiles(List<File> files, String targetHost, int targetPort, Consumer<Double> progressCallback) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Expand directories to individual files
            List<File> allFiles = expandDirectories(files);

            for (int i = 0; i < allFiles.size(); i++) {
                File file = allFiles.get(i);
                String relativePath = getRelativePath(file, files);
                sendSingleFile(httpClient, file, targetHost, targetPort, relativePath);

                double progress = (double) (i + 1) / allFiles.size();
                progressCallback.accept(progress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<File> expandDirectories(List<File> files) {
        List<File> allFiles = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                allFiles.addAll(getAllFilesInDirectory(file));
            } else {
                allFiles.add(file);
            }
        }

        return allFiles;
    }

    private List<File> getAllFilesInDirectory(File directory) {
        List<File> fileList = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            paths.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(fileList::add);
        } catch (IOException e) {
            System.err.println("Error walking directory: " + directory.getAbsolutePath());
            e.printStackTrace();
        }

        return fileList;
    }

    private String getRelativePath(File file, List<File> originalFiles) {
        // Find which original file/directory this file belongs to
        for (File original : originalFiles) {
            if (original.isDirectory()) {
                Path originalPath = original.toPath();
                Path filePath = file.toPath();

                if (filePath.startsWith(originalPath)) {
                    return original.getName() + "/" + originalPath.relativize(filePath).toString().replace("\\", "/");
                }
            } else if (original.equals(file)) {
                return file.getName();
            }
        }

        return file.getName();
    }

    public void sendSingleFile(CloseableHttpClient httpClient, File file, String targetHost, int targetPort) {
        sendSingleFile(httpClient, file, targetHost, targetPort, file.getName());
    }

    public void sendSingleFile(CloseableHttpClient httpClient, File file, String targetHost, int targetPort, String relativePath) {
        String url = "http://" + targetHost + ":" + targetPort + "/upload";
        System.out.println("Sending file to: " + url);
        System.out.println("File: " + file.getAbsolutePath() + " -> " + relativePath);

        HttpPost post = new HttpPost(url);

        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
                .addTextBody("relativePath", relativePath, ContentType.TEXT_PLAIN)
                .build();

        post.setEntity(entity);

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            System.out.println("Response Code: " + response.getCode());
            System.out.println("Response Reason: " + response.getReasonPhrase());

            if (response.getEntity() != null) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                System.out.println("Response Body: " + responseBody);
            }
        } catch (IOException e) {
            System.err.println("Failed to send file:");
            e.printStackTrace();
        }
    }
}