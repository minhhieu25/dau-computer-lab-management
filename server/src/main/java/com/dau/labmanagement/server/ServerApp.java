package com.dau.labmanagement.server;

import com.dau.labmanagement.server.config.AppConfig;
import com.dau.labmanagement.server.config.DataSourceProvider;
import com.dau.labmanagement.server.config.DatabaseMigrator;
import com.dau.labmanagement.server.network.ServerNetworkRuntime;
import com.zaxxer.hikari.HikariDataSource;
import com.dau.labmanagement.server.config.JdbiProvider;
import com.dau.labmanagement.server.model.service.dashboard.DashboardDatabaseService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import com.dau.labmanagement.server.controller.ServerShellController;
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
    private DashboardDatabaseService dashboardDatabaseService;

    @Override
    public void start(Stage stage) throws IOException {
        bootstrapDatabase();
        networkRuntime = new ServerNetworkRuntime();
        networkRuntime.start();

        if (dataSource != null) {
            dashboardDatabaseService =
                    new DashboardDatabaseService(
                            JdbiProvider.create(dataSource),
                            networkRuntime.getMachineRegistry());

            dashboardDatabaseService.start();
        }

        FXMLLoader loader = new FXMLLoader(
                ServerApp.class.getResource("/fxml/server-shell.fxml"));

        Parent root = loader.load();

        ServerShellController controller = loader.getController();

        controller.setMachineRegistry(
                networkRuntime.getMachineRegistry());
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

        if (dashboardDatabaseService != null) {
            dashboardDatabaseService.close();
        }

        try {
            if (networkRuntime != null) {
                networkRuntime.close();
            }
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }


    private void bootstrapDatabase() {
        AppConfig config = AppConfig.fromEnv();
        if (config.dbPassword() == null || config.dbPassword().isBlank()) {
            LOG.warn("DB_PASSWORD is not set; skipping database bootstrap");
            return;
        }
        try {
            dataSource = DataSourceProvider.create(config);
            int applied = DatabaseMigrator.migrate(dataSource, config.migrationLocations());
            LOG.info("Database ready at {}; Flyway applied {} migration(s)", config.dbUrl(), applied);
        } catch (Exception e) {
            LOG.error("Database bootstrap failed", e);

            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
