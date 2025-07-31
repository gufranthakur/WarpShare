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
        BorderPane.setAlignment(receiverLabel, Pos.CENTER);
        BorderPane.setMargin(receiverLabel, new javafx.geometry.Insets(20, 0, 10, 0));

        ListView<File> fileList = new ListView<>(selectedFiles);

        Button selectFilesButton = new Button("Select Files");
        Button selectFolderButton = new Button("Select Folders");
        Button sendButton = new Button("Send");

        progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(300);

        Button backButton = new Button("Back");

        selectFilesButton.setOnAction(e -> selectFiles());
        selectFolderButton.setOnAction(e -> selectFolders());
        sendButton.setOnAction(e -> sendFiles());
        backButton.setOnAction(e -> app.showSearchReceiverPanel());

        HBox selectionButtons = new HBox(10);
        selectionButtons.setAlignment(Pos.CENTER);
        selectionButtons.getChildren().addAll(selectFilesButton, selectFolderButton);

        VBox center = new VBox(15);
        center.setAlignment(Pos.CENTER);
        center.setSpacing(15);
        center.setStyle("-fx-padding: 30;");
        center.getChildren().addAll(fileList, selectionButtons, sendButton);

        HBox bottom = new HBox(15);
        bottom.setAlignment(Pos.CENTER);
        bottom.setSpacing(15);
        bottom.setStyle("-fx-padding: 20;");
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

    public void selectFolders() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Send");
        File folder = directoryChooser.showDialog(app.primaryStage);
        if (folder != null) {
            selectedFiles.add(folder);
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