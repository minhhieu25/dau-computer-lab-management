package com.dau.labmanagement.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** JavaFX entry point for the server shell. */
public final class ServerApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(ServerApp.class.getResource("/fxml/server-shell.fxml"));
        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(ServerApp.class.getResource("/css/server.css").toExternalForm());
        stage.setTitle("DAU Lab Management - Server");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}