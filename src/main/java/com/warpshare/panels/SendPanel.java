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
        root.setStyle("-fx-background-color: #f8f9fa;");

        Label receiverLabel = new Label("Sending to: " + receiverName);
        receiverLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 20px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #333333;");
        BorderPane.setAlignment(receiverLabel, Pos.CENTER);
        BorderPane.setMargin(receiverLabel, new javafx.geometry.Insets(20, 0, 10, 0));

        ListView<File> fileList = new ListView<>(selectedFiles);
        fileList.setStyle("-fx-border-color: #ced4da; -fx-background-radius: 8;");

        Button selectFilesButton = new Button("Select Files");
        styleButton(selectFilesButton, "#0d6efd");

        Button sendButton = new Button("Send");
        styleButton(sendButton, "#198754");

        progressBar = new ProgressBar(0.0);
        progressBar.setStyle("-fx-accent: #0d6efd;");
        progressBar.setPrefWidth(300);

        Button backButton = new Button("Back");
        styleButton(backButton, "#6c757d");

        selectFilesButton.setOnAction(e -> selectFiles());
        sendButton.setOnAction(e -> sendFiles());
        backButton.setOnAction(e -> app.showSearchReceiverPanel());

        VBox center = new VBox(15);
        center.setAlignment(Pos.CENTER);
        center.setStyle("-fx-padding: 30;");
        center.getChildren().addAll(fileList, selectFilesButton, sendButton);

        HBox bottom = new HBox(15);
        bottom.setAlignment(Pos.CENTER);
        bottom.setStyle("-fx-padding: 20;");
        bottom.getChildren().addAll(progressBar, backButton);

        root.setTop(receiverLabel);
        root.setCenter(center);
        root.setBottom(bottom);
    }

    private void styleButton(Button button, String bgColor) {
        button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20 10 20;"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: derive(" + bgColor + ", 20%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20 10 20;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20 10 20;"
        ));
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
            String serviceName = receiverService.getName();
            if (serviceName.startsWith("Manual-")) {
                targetHost = serviceName.substring(7);
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
