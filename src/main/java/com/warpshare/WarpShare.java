package com.warpshare;

import atlantafx.base.theme.CupertinoDark;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.warpshare.panels.HomePanel;
import com.warpshare.services.HttpServerService;

import javax.jmdns.ServiceInfo;

public class WarpShare extends Application {
    public Stage primaryStage;
    public StackPane rootPane;
    public Scene scene;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
        this.primaryStage = stage;
        primaryStage.setTitle("WarpShare");

        rootPane = new StackPane();
        scene = new Scene(rootPane, 800, 600);
        primaryStage.setScene(scene);

        showHomePanel();
        primaryStage.show();
    }

    public void showHomePanel() {
        rootPane.getChildren().clear();
        rootPane.getChildren().add(new HomePanel(this).getRoot());
    }

    public void showSearchReceiverPanel() {
        rootPane.getChildren().clear();
        rootPane.getChildren().add(new com.warpshare.panels.SearchReceiverPanel(this).getRoot());
    }

    public void showSendPanel(String receiverName, ServiceInfo receiverService) {
        rootPane.getChildren().clear();
        rootPane.getChildren().add(new com.warpshare.panels.SendPanel(this, receiverName, receiverService).getRoot());
    }

    public void showSearchSenderPanel() {
        rootPane.getChildren().clear();
        rootPane.getChildren().add(new com.warpshare.panels.SearchSenderPanel(this).getRoot());
    }

    public void showReceivePanel(String senderName, ObservableList<String> receivedFiles, HttpServerService httpServer) {
        rootPane.getChildren().clear();
        rootPane.getChildren().add(new com.warpshare.panels.ReceivePanel(this, senderName, receivedFiles, httpServer).getRoot());
    }

    public static void main(String[] args) {
        launch(args);
    }
}