package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.NetworkService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.warpshare.services.NetworkService.SERVICE_TYPE;

public class SearchReceiverPanel {
    public WarpShare app;
    public BorderPane root;
    public ObservableList<DeviceInfo> devices;
    public NetworkService networkService;
    public Map<String, ServiceInfo> serviceMap;
    private JmDNS jmdns;
    private ExecutorService executor;
    private ListView<DeviceInfo> deviceListView;
    private Button refreshButton;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;

    public static class DeviceInfo {
        public String ipAddress;
        public String hostname;
        public boolean isActive;
        public ServiceInfo serviceInfo;

        public DeviceInfo(String ipAddress, String hostname, boolean isActive) {
            this.ipAddress = ipAddress;
            this.hostname = hostname;
            this.isActive = isActive;
        }

        @Override
        public String toString() {
            return hostname + " (" + ipAddress + ")";
        }
    }

    public SearchReceiverPanel(WarpShare app) {
        this.app = app;
        this.devices = FXCollections.observableArrayList();
        this.networkService = new NetworkService();
        this.serviceMap = new HashMap<>();
        this.executor = Executors.newCachedThreadPool();
        createPanel();
        startDiscovery();
        startNetworkScan();
    }

    public void createPanel() {
        root = new BorderPane();
        root.setPadding(new Insets(20));

        statusLabel = new Label("Scanning network for devices...");
        statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 16px; " +
                "-fx-text-fill: #6c757d;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(-1);
        progressIndicator.setStyle("-fx-accent: #007bff; " +
                "-fx-pref-width: 30; " +
                "-fx-pref-height: 30;");

        refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #28a745; " +
                "-fx-text-fill: white; " +
                "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 8 16 8 16;");
        refreshButton.setOnAction(e -> refreshDevices());

        TextField ipField = new TextField("192.168.");
        ipField.setPromptText("Enter IP address");
        ipField.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 6; " +
                "-fx-border-radius: 6; " +
                "-fx-padding: 8;");

        TextField portField = new TextField("8443");
        portField.setPromptText("Port");
        portField.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 6; " +
                "-fx-border-radius: 6; " +
                "-fx-padding: 8; " +
                "-fx-pref-width: 80;");

        Button connectButton = new Button("Connect Manually");
        connectButton.setStyle("-fx-background-color: #007bff; " +
                "-fx-text-fill: white; " +
                "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 8 16 8 16;");

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

        Label ipLabel = new Label("IP:");
        ipLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px;");

        Label portLabel = new Label("Port:");
        portLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px;");

        HBox manualSection = new HBox(10);
        manualSection.setAlignment(Pos.CENTER);
        manualSection.setPadding(new Insets(10, 0, 10, 0));
        manualSection.getChildren().addAll(ipLabel, ipField, portLabel, portField, connectButton);

        deviceListView = new ListView<>(devices);
        deviceListView.setCellFactory(listView -> new DeviceListCell());
        deviceListView.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8;");

        Button backButton = new Button("Back");
        backButton.setStyle("-fx-background-color: #6c757d; " +
                "-fx-text-fill: white; " +
                "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 12 24 12 24; " +
                "-fx-pref-width: 100;");

        deviceListView.setOnMouseClicked(e -> {
            DeviceInfo selected = deviceListView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isActive && selected.serviceInfo != null) {
                app.showSendPanel(selected.toString(), selected.serviceInfo);
            } else if (selected != null && !selected.isActive) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Device Inactive");
                alert.setHeaderText(null);
                alert.setContentText("This device is not running WarpShare or is not discoverable.");
                alert.showAndWait();
            }
        });

        backButton.setOnAction(e -> {
            cleanup();
            app.showHomePanel();
        });

        HBox topSection = new HBox(15);
        topSection.setAlignment(Pos.CENTER_LEFT);
        topSection.setPadding(new Insets(0, 0, 10, 0));
        topSection.getChildren().addAll(statusLabel, progressIndicator, refreshButton);

        Label devicesLabel = new Label("Network devices:");
        devicesLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold;");

        VBox center = new VBox(15);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(10));
        center.getChildren().addAll(topSection, manualSection, devicesLabel, deviceListView);

        VBox bottomContainer = new VBox();
        bottomContainer.setAlignment(Pos.CENTER);
        bottomContainer.setPadding(new Insets(15, 0, 0, 0));
        bottomContainer.getChildren().add(backButton);

        root.setCenter(center);
        root.setBottom(bottomContainer);
    }

    private static class DeviceListCell extends ListCell<DeviceInfo> {
        @Override
        protected void updateItem(DeviceInfo device, boolean empty) {
            super.updateItem(device, empty);
            if (empty || device == null) {
                setText(null);
                setTextFill(Color.BLACK);
                setStyle("");
                setGraphic(null);
            } else {
                setText(device.toString());
                String baseStyle = "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10;";

                HBox statusBox = new HBox(10);
                statusBox.setAlignment(Pos.CENTER_LEFT);

                Label deviceLabel = new Label(device.toString());
                deviceLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                        "-fx-font-size: 14px;");

                HBox statusIndicator = new HBox(5);
                statusIndicator.setAlignment(Pos.CENTER);

                if (device.isActive) {
                    deviceLabel.setTextFill(Color.web("#28a745"));
                    setStyle(baseStyle + "-fx-background-color: rgba(40, 167, 69, 0.1);");

                    try {
                        Image onlineIcon = new Image(getClass().getResourceAsStream("/online.png"));
                        ImageView iconView = new ImageView(onlineIcon);
                        iconView.setFitWidth(16);
                        iconView.setFitHeight(16);
                        statusIndicator.getChildren().add(iconView);
                    } catch (Exception e) {
                        System.err.println("Could not load online.png icon");
                    }

                    Label activeLabel = new Label("Active");
                    activeLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 12px; " +
                            "-fx-text-fill: #28a745; " +
                            "-fx-font-weight: bold;");
                    statusIndicator.getChildren().add(activeLabel);
                } else {
                    deviceLabel.setTextFill(Color.web("#dc3545"));
                    setStyle(baseStyle + "-fx-background-color: rgba(220, 53, 69, 0.1);");

                    try {
                        Image offlineIcon = new Image(getClass().getResourceAsStream("/offline.png"));
                        ImageView iconView = new ImageView(offlineIcon);
                        iconView.setFitWidth(16);
                        iconView.setFitHeight(16);
                        statusIndicator.getChildren().add(iconView);
                    } catch (Exception e) {
                        System.err.println("Could not load offline.png icon");
                    }

                    Label inactiveLabel = new Label("Inactive");
                    inactiveLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 12px; " +
                            "-fx-text-fill: #dc3545; " +
                            "-fx-font-weight: bold;");
                    statusIndicator.getChildren().add(inactiveLabel);
                }

                statusBox.getChildren().addAll(deviceLabel, new Region(), statusIndicator);
                HBox.setHgrow(statusBox.getChildren().get(1), Priority.ALWAYS);

                setText(null);
                setGraphic(statusBox);
            }
        }
    }

    private ServiceInfo createManualServiceInfo(String ip, int port) {
        return ServiceInfo.create("_warpshare._tcp.local.", "Manual-" + ip, port, 0, 0, new HashMap<>());
    }

    private void refreshDevices() {
        statusLabel.setText("Refreshing devices...");
        refreshButton.setDisable(true);

        Map<String, DeviceInfo> currentDevices = new HashMap<>();
        for (DeviceInfo device : devices) {
            currentDevices.put(device.ipAddress, device);
        }

        Platform.runLater(() -> devices.clear());

        Task<Void> refreshTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                scanNetwork(currentDevices);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    statusLabel.setText("Scan complete. Found " + devices.size() + " devices.");
                    progressIndicator.setVisible(false);
                    refreshButton.setDisable(false);
                });
            }
        };

        executor.submit(refreshTask);
    }

    public void startNetworkScan() {
        Task<Void> scanTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                scanNetwork(new HashMap<>());
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    statusLabel.setText("Scan complete. Found " + devices.size() + " devices.");
                });
            }
        };

        executor.submit(scanTask);
    }

    private void scanNetwork(Map<String, DeviceInfo> preserveDevices) throws Exception {
        String localIP = getLocalIPAddress();
        if (localIP == null) return;

        String subnet = localIP.substring(0, localIP.lastIndexOf('.'));
        ExecutorService scanExecutor = Executors.newFixedThreadPool(50);

        for (int i = 1; i <= 254; i++) {
            final String targetIP = subnet + "." + i;

            scanExecutor.submit(() -> {
                try {
                    InetAddress target = InetAddress.getByName(targetIP);

                    boolean isReachable = target.isReachable(2000);

                    String hostname = target.getCanonicalHostName();
                    boolean hasHostname = !hostname.equals(targetIP);

                    if (isReachable || hasHostname) {
                        if (hostname.equals(targetIP)) {
                            hostname = "Device-" + targetIP.substring(targetIP.lastIndexOf('.') + 1);
                        }

                        DeviceInfo existingDevice = preserveDevices.get(targetIP);
                        DeviceInfo device;
                        if (existingDevice != null) {
                            device = new DeviceInfo(targetIP, hostname, existingDevice.isActive);
                            device.serviceInfo = existingDevice.serviceInfo;
                        } else {
                            device = new DeviceInfo(targetIP, hostname, false);
                        }

                        Platform.runLater(() -> {
                            devices.add(device);
                            statusLabel.setText("Found " + devices.size() + " devices...");
                        });
                    }
                } catch (IOException e) {
                }
            });
        }

        scanExecutor.shutdown();
        scanExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
    }

    private String getLocalIPAddress() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isLoopback() && ni.isUp() && !ni.isVirtual()) {
                    for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void startDiscovery() {
        try {
            ServiceListener listener = new ServiceListener() {
                public void serviceAdded(ServiceEvent event) {
                    System.out.println("Service added: " + event.getName());
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 5000);
                }

                public void serviceRemoved(ServiceEvent event) {
                    Platform.runLater(() -> {
                        String serviceName = event.getName();
                        ServiceInfo info = serviceMap.remove(serviceName);

                        if (info != null) {
                            String serviceIP = info.getServer();
                            for (DeviceInfo device : devices) {
                                if (device.ipAddress.equals(serviceIP)) {
                                    device.isActive = false;
                                    device.serviceInfo = null;
                                    break;
                                }
                            }
                            deviceListView.refresh();
                        }
                    });
                }

                public void serviceResolved(ServiceEvent event) {
                    Platform.runLater(() -> {
                        String serviceName = event.getName();
                        ServiceInfo info = event.getInfo();
                        serviceMap.put(serviceName, info);

                        System.out.println("Service resolved: " + serviceName);

                        String serviceIP = info.getServer();
                        if (serviceIP != null) {
                            boolean found = false;

                            for (DeviceInfo device : devices) {
                                if (device.ipAddress.equals(serviceIP)) {
                                    device.isActive = true;
                                    device.serviceInfo = info;
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                DeviceInfo newDevice = new DeviceInfo(serviceIP, serviceName, true);
                                newDevice.serviceInfo = info;
                                devices.add(newDevice);
                            }

                            deviceListView.refresh();
                        }
                    });
                }
            };

            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isLoopback() && ni.isUp() && !ni.isVirtual()) {
                    for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            jmdns = JmDNS.create(addr);
                            jmdns.addServiceListener(SERVICE_TYPE, listener);
                            System.out.println("Started mDNS discovery on: " + addr.getHostAddress());
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

    private void cleanup() {
        try {
            if (jmdns != null) {
                jmdns.close();
            }
            networkService.stop();
            if (executor != null) {
                executor.shutdown();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public BorderPane getRoot() {
        return root;
    }
}