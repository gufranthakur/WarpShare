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
import java.util.List;
import java.util.function.Consumer;

public class HttpClientService {

    public void sendFiles(List<File> files, String targetHost, int targetPort, Consumer<Double> progressCallback) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                sendSingleFile(httpClient, file, targetHost, targetPort);

                double progress = (double) (i + 1) / files.size();
                progressCallback.accept(progress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendSingleFile(CloseableHttpClient httpClient, File file, String targetHost, int targetPort) {
        String url = "http://" + targetHost + ":" + targetPort + "/upload";
        System.out.println("Sending file to: " + url);
        System.out.println("File: " + file.getAbsolutePath());

        HttpPost post = new HttpPost(url);

        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
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