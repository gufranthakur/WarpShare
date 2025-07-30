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

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.warpshare.services.NetworkService.SERVICE_TYPE;

public class SearchReceiverPanel {
    public WarpShare app;
    public BorderPane root;
    public ObservableList<String> receivers;
    public NetworkService networkService;
    public Map<String, ServiceInfo> serviceMap;
    private JmDNS jmdns;

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
                if (jmdns != null) {
                    jmdns.close();
                }
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
            ServiceListener listener = new ServiceListener() {
                public void serviceAdded(ServiceEvent event) {
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 5000);
                }
                public void serviceRemoved(ServiceEvent event) {
                    Platform.runLater(() -> {
                        String serviceName = event.getName();
                        receivers.remove(serviceName);
                        serviceMap.remove(serviceName);
                    });
                }
                public void serviceResolved(ServiceEvent event) {
                    Platform.runLater(() -> {
                        String serviceName = event.getName();
                        receivers.add(serviceName);
                        serviceMap.put(serviceName, event.getInfo());
                    });
                }
            };

            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isLoopback() && ni.isUp() && !ni.isVirtual()) {
                    for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            jmdns = JmDNS.create(addr);
                            jmdns.addServiceListener(SERVICE_TYPE, listener);
                            break;
                        }
                    }
                    if (jmdns != null) break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BorderPane getRoot() {
        return root;
    }
}