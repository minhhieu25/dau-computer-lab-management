package com.dau.labmanagement.server;

import com.dau.labmanagement.server.config.AppConfig;
import com.dau.labmanagement.server.config.DataSourceProvider;
import com.dau.labmanagement.server.config.DatabaseMigrator;
import com.dau.labmanagement.server.config.JdbiProvider;
import com.dau.labmanagement.server.network.ServerNetworkRuntime;
import com.zaxxer.hikari.HikariDataSource;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Entry point of the PC Server (teacher/admin desktop application).
 */
public final class ServerApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ServerApp.class);
    private ServerNetworkRuntime networkRuntime;
    private HikariDataSource dataSource;

    @Override
    public void start(Stage stage) throws IOException {
        AppConfig config = AppConfig.fromEnv();
        dataSource = DataSourceProvider.create(config);
        int applied;
        try {
            applied = DatabaseMigrator.migrate(dataSource, config.migrationLocations());
        } catch (RuntimeException exception) {
            dataSource.close();
            dataSource = null;
            throw exception;
        }
        LOG.info("Database ready at {}; Flyway applied {} migration(s)", config.dbUrl(), applied);
        networkRuntime = new ServerNetworkRuntime(JdbiProvider.create(dataSource));
        try {
            networkRuntime.start();
        } catch (IOException | RuntimeException exception) {
            dataSource.close();
            dataSource = null;
            networkRuntime = null;
            throw exception;
        }

        Parent root = FXMLLoader.load(ServerApp.class.getResource("/fxml/server-shell.fxml"));
        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(ServerApp.class.getResource("/css/server.css").toExternalForm());
        stage.setTitle("DAU Lab Management - Server");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws IOException {
        IOException closeFailure = null;
        try {
            if (networkRuntime != null) {
                networkRuntime.close();
            }
        } catch (IOException exception) {
            closeFailure = exception;
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
