package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.NetworkService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import javax.jmdns.ServiceInfo;
import java.util.HashMap;
import java.util.Map;

public class SearchReceiverPanel {
    public WarpShare app;
    public BorderPane root;
    public ObservableList<String> receivers;
    public NetworkService networkService;
    public Map<String, ServiceInfo> serviceMap;

    public SearchReceiverPanel(WarpShare app) {
        this.app = app;
        this.receivers = FXCollections.observableArrayList();
        this.networkService = new NetworkService();
        this.serviceMap = new HashMap<>();
        createPanel();
        startDiscovery();
    }

    public void createPanel() {
        root = new BorderPane();

        Label title = new Label("Discovering Receivers...");
        ListView<String> deviceList = new ListView<>(receivers);
        Button backButton = new Button("Back");

        deviceList.setOnMouseClicked(e -> {
            String selected = deviceList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ServiceInfo service = serviceMap.get(selected);
                app.showSendPanel(selected, service);
            }
        });

        backButton.setOnAction(e -> {
            try {
                networkService.stop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            app.showHomePanel();
        });

        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(title, deviceList);

        root.setCenter(center);
        root.setBottom(backButton);
    }

    public void startDiscovery() {
        try {
            networkService.startDiscovery(serviceInfo -> {
                Platform.runLater(() -> {
                    String deviceName = serviceInfo.getName();
                    if (!receivers.contains(deviceName)) {
                        receivers.add(deviceName);
                        serviceMap.put(deviceName, serviceInfo);
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BorderPane getRoot() {
        return root;
    }
}