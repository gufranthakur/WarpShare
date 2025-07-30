package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.HttpServerService;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ReceivePanel {
    public WarpShare app;
    public String senderName;
    public BorderPane root;
    public ObservableList<String> receivedFiles;
    public ProgressBar progressBar;
    public HttpServerService httpServer;

    public ReceivePanel(WarpShare app, String senderName, ObservableList<String> receivedFiles, HttpServerService httpServer) {
        this.app = app;
        this.senderName = senderName;
        this.receivedFiles = receivedFiles;
        this.httpServer = httpServer;
        createPanel();
    }

    public void createPanel() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #f8f9fa;");

        Label senderLabel = new Label("Receiving from: " + senderName);
        senderLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 20px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #333333;");
        BorderPane.setAlignment(senderLabel, Pos.CENTER);
        BorderPane.setMargin(senderLabel, new javafx.geometry.Insets(20, 0, 10, 0));

        ListView<String> fileList = new ListView<>(receivedFiles);
        fileList.setStyle("-fx-border-color: #ced4da; -fx-background-radius: 8;");

        progressBar = new ProgressBar(0.0);
        progressBar.setStyle("-fx-accent: #0d6efd;");
        progressBar.setPrefWidth(300);

        Button backButton = new Button("Back");
        styleButton(backButton, "#6c757d");

        backButton.setOnAction(e -> {
            if (httpServer != null) {
                httpServer.stop();
            }
            app.showSearchSenderPanel();
        });

        VBox center = new VBox(15);
        center.setAlignment(Pos.CENTER);
        center.setStyle("-fx-padding: 30;");
        center.getChildren().add(fileList);

        HBox bottom = new HBox(15);
        bottom.setAlignment(Pos.CENTER);
        bottom.setStyle("-fx-padding: 20;");
        bottom.getChildren().addAll(progressBar, backButton);

        root.setTop(senderLabel);
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

    public BorderPane getRoot() {
        return root;
    }
}
