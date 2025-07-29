package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.HttpServerService;
import com.warpshare.services.NetworkService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class SearchSenderPanel {
    public WarpShare app;
    public BorderPane root;
    public NetworkService networkService;
    public HttpServerService httpServer;
    public ObservableList<String> receivedFiles;
    public static final int SERVER_PORT = 8443;

    public SearchSenderPanel(WarpShare app) {
        this.app = app;
        this.networkService = new NetworkService();
        this.httpServer = new HttpServerService();
        this.receivedFiles = FXCollections.observableArrayList();
        createPanel();
        startListening();
    }

    public void createPanel() {
        root = new BorderPane();

        Label title = new Label("Waiting for sender...");
        Label status = new Label("Listening on port " + SERVER_PORT);
        Button backButton = new Button("Back");

        backButton.setOnAction(e -> {
            stopServices();
            app.showHomePanel();
        });

        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(title, status);

        root.setCenter(center);
        root.setBottom(backButton);
    }

    public void startListening() {
        try {
            httpServer.startServer(SERVER_PORT, receivedFiles, progress -> {
                if (!receivedFiles.isEmpty()) {
                    app.showReceivePanel("Remote Sender", receivedFiles, httpServer);
                }
            });

            String deviceName = "WarpShare-" + System.getProperty("user.name");
            networkService.startAdvertising(SERVER_PORT, deviceName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopServices() {
        try {
            if (httpServer != null) httpServer.stop();
            if (networkService != null) networkService.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BorderPane getRoot() {
        return root;
    }
}