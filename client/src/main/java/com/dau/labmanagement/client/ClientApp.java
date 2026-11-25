package com.dau.labmanagement.client;

import com.dau.labmanagement.client.controller.ClientShellController;
import com.dau.labmanagement.client.model.net.ClientNetworkRuntime;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** JavaFX entry point for the client shell and network runtime. */
public final class ClientApp extends Application {

    private ClientNetworkRuntime networkRuntime;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                ClientApp.class.getResource("/fxml/client-shell.fxml"));
        Parent root = loader.load();
        ClientShellController controller = loader.getController();
        networkRuntime = new ClientNetworkRuntime(controller::setNetworkStatus);
        networkRuntime.start();

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(ClientApp.class.getResource("/css/client.css").toExternalForm());
        stage.setTitle("DAU Lab Management - Client");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
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
