package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.NetworkService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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

        statusLabel = new Label("Scanning network for devices...");

        refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshDevices());

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

        deviceListView = new ListView<>(devices);
        deviceListView.setCellFactory(listView -> new DeviceListCell());
        Button backButton = new Button("Back");

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

        HBox topSection = new HBox(10);
        topSection.setAlignment(Pos.CENTER);
        topSection.getChildren().addAll(statusLabel, refreshButton);

        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER);
        center.getChildren().addAll(topSection, manualSection, new Label("Network devices:"), deviceListView);

        root.setCenter(center);
        root.setBottom(backButton);
    }

    private static class DeviceListCell extends ListCell<DeviceInfo> {
        @Override
        protected void updateItem(DeviceInfo device, boolean empty) {
            super.updateItem(device, empty);
            if (empty || device == null) {
                setText(null);
                setTextFill(Color.BLACK);
            } else {
                setText(device.toString());
                if (device.isActive) {
                    setTextFill(Color.GREEN);
                } else {
                    setTextFill(Color.RED);
                }
            }
        }
    }

    private ServiceInfo createManualServiceInfo(String ip, int port) {
        return ServiceInfo.create("_warpshare._tcp.local.", "Manual-" + ip, port, 0, 0, new HashMap<>());
    }

    private void refreshDevices() {
        statusLabel.setText("Refreshing devices...");
        refreshButton.setDisable(true);

        // Preserve active states
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

                    // Try multiple methods to detect devices
                    boolean isReachable = target.isReachable(2000);

                    // Even if not reachable by ping, try to resolve hostname
                    String hostname = target.getCanonicalHostName();
                    boolean hasHostname = !hostname.equals(targetIP);

                    if (isReachable || hasHostname) {
                        if (hostname.equals(targetIP)) {
                            hostname = "Device-" + targetIP.substring(targetIP.lastIndexOf('.') + 1);
                        }

                        // Check if we had this device before and preserve its state
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
                    // Ignore unreachable hosts
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

                        // Mark device as inactive
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

                            // Update existing device
                            for (DeviceInfo device : devices) {
                                if (device.ipAddress.equals(serviceIP)) {
                                    device.isActive = true;
                                    device.serviceInfo = info;
                                    found = true;
                                    break;
                                }
                            }

                            // Add new device if not found in scan
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