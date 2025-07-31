package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.HttpServerService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ReceivePanel {
    public WarpShare app;
    public String senderName;
    public BorderPane root;
    public ObservableList<String> receivedFiles;
    public HttpServerService httpServer;
    private static Label statusLabel;

    public ReceivePanel(WarpShare app, String senderName, ObservableList<String> receivedFiles, HttpServerService httpServer) {
        this.app = app;
        this.senderName = senderName;
        this.receivedFiles = receivedFiles;
        this.httpServer = httpServer;
        createPanel();
    }

    public void createPanel() {
        root = new BorderPane();

        // Header section
        VBox headerSection = new VBox(10);
        headerSection.setAlignment(Pos.CENTER);
        headerSection.setPadding(new Insets(20, 0, 10, 0));

        // Receiver icon
        try {
            Image receiverIcon = new Image(getClass().getResourceAsStream("/receiver.png"));
            ImageView iconView = new ImageView(receiverIcon);
            iconView.setFitHeight(64);
            iconView.setPreserveRatio(true);
            headerSection.getChildren().add(iconView);
        } catch (Exception e) {
            System.err.println("Could not load receiver icon");
        }

        Label titleLabel = new Label("File Receiver");
        titleLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 28px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #037bfc;");

        Label senderLabel = new Label("Receiving from: " + senderName);
        senderLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 16px; " +
                "-fx-text-fill: white;");

        headerSection.getChildren().addAll(titleLabel, senderLabel);

        // Status message
        statusLabel = new Label("📥 Waiting for files...");
        statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-text-fill: #007bff; " +
                "-fx-padding: 10; " +
                "-fx-background-color: rgba(0, 123, 255, 0.1); " +
                "-fx-background-radius: 6; " +
                "-fx-border-radius: 6;");
        statusLabel.setVisible(true);

        // Files list
        Label filesLabel = new Label("Received Files:");
        filesLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 18px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: white;");

        ListView<String> fileList = new ListView<>(receivedFiles);
        fileList.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-border-color: #dee2e6; " +
                "-fx-border-width: 1; " +
                "-fx-background-color: white;");
        fileList.setPrefHeight(300);

        VBox center = new VBox(15);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(10, 30, 20, 30));
        center.getChildren().addAll(statusLabel, filesLabel, fileList);

        // Bottom section with styled button
        HBox bottomSection = new HBox();
        bottomSection.setAlignment(Pos.CENTER);
        bottomSection.setPadding(new Insets(15, 0, 25, 0));

        Button backButton = new Button("Back to Home");

        backButton.setStyle("-fx-background-color: #037bfc; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-pref-width: 160;");

        backButton.setOnAction(e -> {
            if (httpServer != null) httpServer.stop();
            app.showSearchSenderPanel();
        });

        bottomSection.getChildren().add(backButton);

        root.setTop(headerSection);
        root.setCenter(center);
        root.setBottom(bottomSection);
    }

    public static void notifyReceiver() {
        Platform.runLater(() -> {
            statusLabel.setText("✓ File received successfully!");
            statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                    "-fx-font-size: 14px; " +
                    "-fx-text-fill: #28a745; " +
                    "-fx-padding: 10; " +
                    "-fx-background-color: rgba(40, 167, 69, 0.1); " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-radius: 6;");
            statusLabel.setVisible(true);
        });

    }

    public BorderPane getRoot() {
        return root;
    }
}