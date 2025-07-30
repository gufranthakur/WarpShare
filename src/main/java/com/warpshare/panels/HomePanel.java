package com.warpshare.panels;

import com.warpshare.WarpShare;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class HomePanel {
    public WarpShare app;
    public VBox root;

    public HomePanel(WarpShare app) {
        this.app = app;
        createPanel();
    }

    public void createPanel() {
        root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50, 40, 50, 40));

        Label title = new Label("WarpShare");
        title.setStyle("-fx-font-weight: bold; " +
                "-fx-font-size: 48px; " +
                "-fx-text-fill: linear-gradient(to right, #0583f2, #9e62fc);");

        Button sendButton = createStyledButton("Send Data", "/sendIcon.png",
                "-fx-background-color: linear-gradient(to bottom, #4CAF50, #388E3C); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 16px; " +
                        "-fx-pref-width: 220; " +
                        "-fx-pref-height: 45; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 10 20 10 20;",
                "-fx-background-color: linear-gradient(to bottom, #45a049, #2E7D32); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: Arial; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 16px; " +
                        "-fx-pref-width: 220; " +
                        "-fx-pref-height: 45; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0.4, 0, 3); " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-scale-x: 1.05; " +
                        "-fx-scale-y: 1.05;");

        Button receiveButton = createStyledButton("Receive Data", "/receiveIcon.png",
                "-fx-background-color: linear-gradient(to bottom, #2196F3, #1565C0); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: Arial; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 16px; " +
                        "-fx-pref-width: 220; " +
                        "-fx-pref-height: 45; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-padding: 10 20 10 20;",
                "-fx-background-color: linear-gradient(to bottom, #1976D2, #0D47A1); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: Arial; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 16px; " +
                        "-fx-pref-width: 220; " +
                        "-fx-pref-height: 45; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-scale-x: 1.05; " +
                        "-fx-scale-y: 1.05;");

        sendButton.setOnAction(e -> app.showSearchReceiverPanel());
        receiveButton.setOnAction(e -> app.showSearchSenderPanel());

        root.getChildren().addAll(title, sendButton, receiveButton);
    }

    private Button createStyledButton(String text, String iconPath, String normalStyle, String hoverStyle) {
        Button button = new Button(text);

        try {
            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            ImageView iconView = new ImageView(icon);
            iconView.setFitWidth(24);
            iconView.setFitHeight(24);
            button.setGraphic(iconView);
        } catch (Exception e) {
            System.err.println("Could not load icon: " + iconPath);
        }

        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));

        return button;
    }

    public VBox getRoot() {
        return root;
    }
}