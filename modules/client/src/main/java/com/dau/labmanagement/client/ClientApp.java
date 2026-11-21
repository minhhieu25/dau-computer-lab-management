package com.dau.labmanagement.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Entry point of the PC Client (student machine).
 *
 * <p>Scaffold only: opens an empty window to prove the JavaFX + Gradle
 * wiring works. The real client runs as a tray application with an overlay
 * stage; login UI (SC_002) arrives at Sprint 1, overlay/focus mode at S2–S3.</p>
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage stage) {
        Label placeholder = new Label("PC Client - scaffold (agent + MVC shell)");
        placeholder.setId("placeholder-label");
        Scene scene = new Scene(placeholder, 600, 400);
        stage.setTitle("DAU Computer Lab Management - Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
