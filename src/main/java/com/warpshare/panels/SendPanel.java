package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.HttpClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
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
    private Label statusLabel;

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

        // Header section
        VBox headerSection = new VBox(10);
        headerSection.setAlignment(Pos.CENTER);
        headerSection.setPadding(new Insets(20, 0, 10, 0));

        // Sender icon
        try {
            Image senderIcon = new Image(getClass().getResourceAsStream("/sender.png"));
            ImageView iconView = new ImageView(senderIcon);
            iconView.setFitHeight(64);
            iconView.setPreserveRatio(true);
            headerSection.getChildren().add(iconView);
        } catch (Exception e) {
            System.err.println("Could not load sender icon");
        }

        Label titleLabel = new Label("File Sender");
        titleLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 28px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #037bfc;");

        Label receiverLabel = new Label("Sending to: " + receiverName);
        receiverLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 16px; " +
                "-fx-text-fill: white;");

        headerSection.getChildren().addAll(titleLabel, receiverLabel);

        // Status message
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10; " +
                "-fx-background-radius: 6; " +
                "-fx-border-radius: 6;");
        statusLabel.setVisible(false);

        // Files list
        Label filesLabel = new Label("Selected Files:");
        filesLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 18px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: white;");

        ListView<File> fileList = new ListView<>(selectedFiles);
        fileList.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-border-color: #dee2e6; " +
                "-fx-border-width: 1;");
        fileList.setPrefHeight(250);

        // Selection buttons
        Button selectFilesButton = new Button("Select Files");
        selectFilesButton.setStyle("-fx-background-color: #28a745; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-pref-width: 140;");

        Button selectFolderButton = new Button("Select Folders");
        selectFolderButton.setStyle("-fx-background-color: #28a745; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-pref-width: 140;");

        Button sendButton = new Button("Send Files");
        sendButton.setStyle("-fx-background-color: #007bff; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-pref-width: 140; ");
        progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(300);
        progressBar.setVisible(false);

        selectFilesButton.setOnAction(e -> selectFiles());
        selectFolderButton.setOnAction(e -> selectFolders());
        sendButton.setOnAction(e -> sendFiles());

        HBox selectionButtons = new HBox(15);
        selectionButtons.setAlignment(Pos.CENTER);
        selectionButtons.getChildren().addAll(selectFilesButton, selectFolderButton);

        VBox center = new VBox(15);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(10, 30, 20, 30));
        center.getChildren().addAll(statusLabel, filesLabel, fileList, selectionButtons, sendButton, progressBar);

        // Bottom section
        HBox bottomSection = new HBox();
        bottomSection.setAlignment(Pos.CENTER);
        bottomSection.setPadding(new Insets(15, 0, 25, 0));

        Button backButton = new Button("Back");
        backButton.setStyle("-fx-background-color: #037bfc; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: 600; " +
                "-fx-pref-width: 100;");

        backButton.setOnAction(e -> app.showSearchReceiverPanel());

        bottomSection.getChildren().add(backButton);

        root.setTop(headerSection);
        root.setCenter(center);
        root.setBottom(bottomSection);
    }

    public void selectFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Send");
        List<File> files = fileChooser.showOpenMultipleDialog(app.primaryStage);
        if (files != null) {
            selectedFiles.addAll(files);
            updateStatus("Files selected: " + selectedFiles.size(), "#28a745");
        }
    }

    public void selectFolders() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Send");
        File folder = directoryChooser.showDialog(app.primaryStage);
        if (folder != null) {
            selectedFiles.add(folder);
            updateStatus("Folder added: " + folder.getName(), "#ffc107", "black");
        }
    }

    public void sendFiles() {
        if (selectedFiles.isEmpty() || receiverService == null) {
            updateStatus("Please select files or folders first", "#dc3545");
            return;
        }

        String targetHost;
        if (receiverService.getInetAddress() != null) {
            targetHost = receiverService.getInetAddress().getHostAddress();
        } else {
            String serviceName = receiverService.getName();
            if (serviceName.startsWith("Manual-")) {
                targetHost = serviceName.substring(7);
            } else {
                updateStatus("❌ Cannot resolve target host", "#dc3545");
                return;
            }
        }

        int targetPort = receiverService.getPort();
        progressBar.setVisible(true);
        updateStatus("Sending files...", "#007bff");

        new Thread(() -> {
            httpClient.sendFiles(selectedFiles, targetHost, targetPort, progress -> {
                Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                    if (progress >= 1.0) {
                        updateStatus("✅ Files sent successfully!", "#28a745");
                        progressBar.setVisible(false);
                    }
                });
            });
        }).start();
    }

    private void updateStatus(String message, String color) {
        updateStatus(message, color, "white");
    }

    private void updateStatus(String message, String color, String textColor) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-padding: 10; " +
                "-fx-background-color: " + color + "20; " +
                "-fx-background-radius: 6; " +
                "-fx-border-radius: 6;");
        statusLabel.setVisible(true);
    }

    public BorderPane getRoot() {
        return root;
    }
}