package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.HttpClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.jmdns.ServiceInfo;
import java.io.File;
import java.util.List;

public class SendPanel {
    public WarpShare app;
    public String receiverName;
    public ServiceInfo receiverService;
    public BorderPane root;
    public ObservableList<File> selectedFiles;
    public ProgressBar progressBar;
    public HttpClientService httpClient;

    public SendPanel(WarpShare app, String receiverName, ServiceInfo receiverService) {
        this.app = app;
        this.receiverName = receiverName;
        this.receiverService = receiverService;
        this.selectedFiles = FXCollections.observableArrayList();
        this.httpClient = new HttpClientService();
        createPanel();
    }

    public void createPanel() {
        root = new BorderPane();

        Label receiverLabel = new Label("Sending to: " + receiverName);

        ListView<File> fileList = new ListView<>(selectedFiles);
        Button selectFilesButton = new Button("Select Files");
        Button sendButton = new Button("Send");

        progressBar = new ProgressBar(0.0);
        Button backButton = new Button("Back");

        selectFilesButton.setOnAction(e -> selectFiles());
        sendButton.setOnAction(e -> sendFiles());
        backButton.setOnAction(e -> app.showSearchReceiverPanel());

        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(fileList, selectFilesButton, sendButton);

        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER);
        bottom.getChildren().addAll(progressBar, backButton);

        root.setTop(receiverLabel);
        root.setCenter(center);
        root.setBottom(bottom);
    }

    public void selectFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Send");
        List<File> files = fileChooser.showOpenMultipleDialog(app.primaryStage);
        if (files != null) {
            selectedFiles.addAll(files);
        }
    }

    public void sendFiles() {
        if (selectedFiles.isEmpty() || receiverService == null) return;

        String targetHost;
        if (receiverService.getInetAddress() != null) {
            targetHost = receiverService.getInetAddress().getHostAddress();
        } else {
            // Extract IP from service name for manual connections
            String serviceName = receiverService.getName();
            if (serviceName.startsWith("Manual-")) {
                targetHost = serviceName.substring(7); // Remove "Manual-" prefix
            } else {
                System.out.println("Cannot resolve target host");
                return;
            }
        }

        int targetPort = receiverService.getPort();

        new Thread(() -> {
            httpClient.sendFiles(selectedFiles, targetHost, targetPort, progress -> {
                Platform.runLater(() -> progressBar.setProgress(progress));
            });
        }).start();
    }

    public BorderPane getRoot() {
        return root;
    }
}