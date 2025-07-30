package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.NetworkService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.jmdns.ServiceInfo;
import java.net.InetAddress;
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

        TextField ipField = new TextField();
        ipField.setPromptText("Enter IP address (e.g., 192.168.1.100)");
        TextField portField = new TextField("8080");
        portField.setPromptText("Port");
        Button connectButton = new Button("Connect Manually");

        connectButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String portText = portField.getText().trim();
            if (!ip.isEmpty() && !portText.isEmpty()) {
                try {
                    int port = Integer.parseInt(portText);
                    ServiceInfo manualService = createManualServiceInfo(ip, port);
                    app.showSendPanel("Manual: " + ip + ":" + port, manualService);
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid port number");
                }
            }
        });

        HBox manualSection = new HBox(10);
        manualSection.setAlignment(Pos.CENTER);
        manualSection.getChildren().addAll(new Label("IP:"), ipField, new Label("Port:"), portField, connectButton);

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
        center.getChildren().addAll(title, manualSection, new Label("Or select discovered device:"), deviceList);

        root.setCenter(center);
        root.setBottom(backButton);
    }

    private ServiceInfo createManualServiceInfo(String ip, int port) {
        return ServiceInfo.create("_warpshare._tcp.local.", "Manual-" + ip, port, 0, 0, new HashMap<>());
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