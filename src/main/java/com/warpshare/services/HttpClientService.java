package com.warpshare.services;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Consumer;

public class HttpClientService {

    public void sendFiles(List<File> files, String targetHost, int targetPort, Consumer<Double> progressCallback) {

        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();


            var sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .build();

            var connManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connManager)
                    .build()) {
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    sendSingleFile(httpClient, file, targetHost, targetPort);

                    double progress = (double) (i + 1) / files.size();
                    progressCallback.accept(progress);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendSingleFile(CloseableHttpClient httpClient, File file, String targetHost, int targetPort)
            throws IOException {

        String url = "https://" + targetHost + ":" + targetPort + "/upload";
        HttpPost post = new HttpPost(url);

        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
                .build();

        post.setEntity(entity);

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            System.out.println("Response: " + response.getCode());
        }
    }
}