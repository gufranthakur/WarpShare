package com.warpshare.panels;

import com.warpshare.WarpShare;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomePanel {
    public WarpShare app;
    public VBox root;

    public HomePanel(WarpShare app) {
        this.app = app;
        createPanel();
    }

    public void createPanel() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        Label title = new Label("WarpShare");
        Button sendButton = new Button("Send Data");
        Button receiveButton = new Button("Receive Data");

        sendButton.setOnAction(e -> app.showSearchReceiverPanel());
        receiveButton.setOnAction(e -> app.showSearchSenderPanel());

        root.getChildren().addAll(title, sendButton, receiveButton);
    }

    public VBox getRoot() {
        return root;
    }
}