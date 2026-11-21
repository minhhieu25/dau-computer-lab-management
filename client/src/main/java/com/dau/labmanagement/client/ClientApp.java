package com.dau.labmanagement.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** JavaFX entry point for the client shell. */
public final class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(ClientApp.class.getResource("/fxml/client-shell.fxml"));
        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(ClientApp.class.getResource("/css/client.css").toExternalForm());
        stage.setTitle("DAU Lab Management - Client");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}