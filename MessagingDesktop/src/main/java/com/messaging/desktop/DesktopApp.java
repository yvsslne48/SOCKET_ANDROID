package com.messaging.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Messaging Desktop");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Handle close request
        primaryStage.setOnCloseRequest(event -> {
            com.messaging.desktop.services.ConnectionService.getInstance().disconnect();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}