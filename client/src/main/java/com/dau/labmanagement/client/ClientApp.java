package com.dau.labmanagement.client;

import com.dau.labmanagement.client.model.net.ClientNetworkRuntime;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Entry point of the PC Client (student machine).
 *
 * <p>Scaffold only: opens an empty window to prove the JavaFX + Gradle
 * wiring works. The real client runs as a tray application with an overlay
 * stage; login UI (SC_002) arrives at Sprint 1, overlay/focus mode at S2–S3.</p>
 */
public class ClientApp extends Application {

    private ClientNetworkRuntime networkRuntime;

    @Override
    public void start(Stage stage) throws IOException {
        Label placeholder = new Label("Searching for the lab server...");
        placeholder.setId("placeholder-label");
        networkRuntime = new ClientNetworkRuntime(
                status -> Platform.runLater(() -> placeholder.setText(status)));
        networkRuntime.start();
        Scene scene = new Scene(placeholder, 600, 400);
        stage.setTitle("DAU Computer Lab Management - Client");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws IOException {
        if (networkRuntime != null) {
            networkRuntime.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
