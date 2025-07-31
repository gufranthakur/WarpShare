package com.warpshare.panels;

import com.warpshare.WarpShare;
import com.warpshare.services.HttpServerService;
import com.warpshare.services.NetworkService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchSenderPanel {
    public WarpShare app;
    public BorderPane root;
    public NetworkService networkService;
    public HttpServerService httpServer;
    public ObservableList<String> receivedFiles;
    public static final int SERVER_PORT = 8443;

    private ExecutorService executor;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private Button backButton;

    public SearchSenderPanel(WarpShare app) {
        this.app = app;
        this.networkService = new NetworkService();
        this.httpServer = new HttpServerService();
        this.receivedFiles = FXCollections.observableArrayList();
        this.executor = Executors.newCachedThreadPool();
        createPanel();
        startListeningAsync();
    }

    public void createPanel() {
        root = new BorderPane();
        root.setPadding(new Insets(30));

        Label title = new Label("Starting WarpShare Service...");
        title.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 24px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #0866ff;");

        statusLabel = new Label("Initializing...");
        statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-text-fill: #6c757d;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(-1);
        progressIndicator.setStyle("-fx-accent: #007bff; " +
                "-fx-pref-width: 50; " +
                "-fx-pref-height: 50;");

        backButton = new Button("Back");
        backButton.setDisable(true);
        backButton.setStyle("-fx-background-color: #6c757d; " +
                "-fx-text-fill: white; " +
                "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: 500; " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 12 24 12 24; " +
                "-fx-pref-width: 100;");

        backButton.setOnAction(e -> {
            stopServices();
            app.showHomePanel();
        });

        VBox center = new VBox(20);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40, 20, 40, 20));
        center.getChildren().addAll(title, statusLabel, progressIndicator);

        VBox bottomContainer = new VBox();
        bottomContainer.setAlignment(Pos.CENTER);
        bottomContainer.setPadding(new Insets(20, 0, 0, 0));
        bottomContainer.getChildren().add(backButton);

        root.setCenter(center);
        root.setBottom(bottomContainer);
    }

    public void startListeningAsync() {
        Task<Void> startupTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> statusLabel.setText("Starting HTTP server..."));

                httpServer.startServer(SERVER_PORT, receivedFiles, progress -> {
                    Platform.runLater(() -> {
                        if (!receivedFiles.isEmpty()) {
                            app.showReceivePanel("Remote Sender", receivedFiles, httpServer);
                        }
                    });
                });

                Thread.sleep(500);

                Platform.runLater(() -> statusLabel.setText("Starting mDNS advertising..."));

                String deviceName = "WarpShare-" + System.getProperty("user.name");
                networkService.startAdvertising(SERVER_PORT, deviceName);

                Thread.sleep(500);

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    statusLabel.setText("Ready! Listening on port " + SERVER_PORT);
                    statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 14px; " +
                            "-fx-text-fill: #28a745;");
                    progressIndicator.setVisible(false);
                    backButton.setDisable(false);
                    backButton.setStyle("-fx-background-color: #007bff; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: 500; " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-padding: 12 24 12 24; " +
                            "-fx-pref-width: 100;");

                    // Replace title
                    Label title = new Label("Waiting for sender...");
                    title.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 24px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #337aff;");

                    // Load and set image
                    ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/receiver.png")));
                    imageView.setFitHeight(100);
                    imageView.setPreserveRatio(true);

                    // Replace the IP address detection section in your succeeded() method with this:

// Fetch IP Address - prioritize WiFi adapters
                    String ipAddress = "Unavailable";
                    try {
                        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                        String fallbackIP = null;

                        while (interfaces.hasMoreElements()) {
                            NetworkInterface iface = interfaces.nextElement();
                            if (iface.isLoopback() || !iface.isUp()) continue;

                            String ifaceName = iface.getName().toLowerCase();
                            String displayName = iface.getDisplayName().toLowerCase();

                            // Check if this is a WiFi/wireless adapter
                            boolean isWiFi = ifaceName.contains("wlan") ||
                                    ifaceName.contains("wifi") ||
                                    ifaceName.contains("wl") ||
                                    displayName.contains("wireless") ||
                                    displayName.contains("wifi") ||
                                    displayName.contains("wi-fi") ||
                                    displayName.contains("lan");

                            Enumeration<InetAddress> addresses = iface.getInetAddresses();
                            while (addresses.hasMoreElements()) {
                                InetAddress addr = addresses.nextElement();
                                if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                                    String currentIP = addr.getHostAddress();

                                    if (isWiFi) {
                                        // Prioritize WiFi adapters
                                        ipAddress = currentIP;
                                        break;
                                    } else if (fallbackIP == null) {
                                        // Keep first valid IP as fallback
                                        fallbackIP = currentIP;
                                    }
                                }
                            }

                            if (isWiFi && !ipAddress.equals("Unavailable")) {
                                break; // Found WiFi IP, stop searching
                            }
                        }

                        // Use fallback if no WiFi adapter found
                        if (ipAddress.equals("Unavailable") && fallbackIP != null) {
                            ipAddress = fallbackIP;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Label ipLabel = new Label("Your IP address is: ");
                    Label ipValue = new Label(ipAddress);
                    ipLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 16px; " +
                            "-fx-text-fill: white;");
                    ipValue.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 16px; " +
                            "-fx-text-fill: #007bff;");

                    VBox ipContainer = new VBox(5, ipLabel, ipValue);
                    ipContainer.setAlignment(Pos.CENTER);

                    VBox center = (VBox) root.getCenter();
                    center.getChildren().set(0, title);
                    center.getChildren().addAll(imageView, ipContainer);
                });
            }


            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to start services");
                    statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 14px; " +
                            "-fx-text-fill: #dc3545;");
                    progressIndicator.setVisible(false);
                    backButton.setDisable(false);
                });
                getException().printStackTrace();
            }
        };

        executor.submit(startupTask);
    }

    public void stopServices() {
        Task<Void> shutdownTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    statusLabel.setText("Stopping services...");
                    statusLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                            "-fx-font-size: 14px; " +
                            "-fx-text-fill: #6c757d;");
                    progressIndicator.setVisible(true);
                    backButton.setDisable(true);
                });

                if (httpServer != null) httpServer.stop();
                if (networkService != null) networkService.stop();

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    if (executor != null) {
                        executor.shutdown();
                    }
                });
            }
        };

        executor.submit(shutdownTask);
    }

    public BorderPane getRoot() {
        return root;
    }
}