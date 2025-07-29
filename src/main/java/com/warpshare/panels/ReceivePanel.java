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

        Label senderLabel = new Label("Receiving from: " + senderName);

        ListView<String> fileList = new ListView<>(receivedFiles);

        progressBar = new ProgressBar(0.0);
        Button backButton = new Button("Back");

        backButton.setOnAction(e -> {
            if (httpServer != null) {
                httpServer.stop();
            }
            app.showSearchSenderPanel();
        });

        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER);
        center.getChildren().add(fileList);

        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER);
        bottom.getChildren().addAll(progressBar, backButton);

        root.setTop(senderLabel);
        root.setCenter(center);
        root.setBottom(bottom);
    }

    public BorderPane getRoot() {
        return root;
    }
}