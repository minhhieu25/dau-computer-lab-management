package com.dau.labmanagement.server;

import com.dau.labmanagement.server.config.AppConfig;
import com.dau.labmanagement.server.config.DataSourceProvider;
import com.dau.labmanagement.server.config.DatabaseMigrator;
import com.dau.labmanagement.server.network.ServerNetworkRuntime;
import com.zaxxer.hikari.HikariDataSource;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Entry point of the PC Server (teacher/admin desktop application).
 *
 * <p>At startup it applies Flyway migrations (FND-02); failures are logged
 * but never crash the UI so the window always opens. The data source is
 * re-created per bootstrap for now — Sprint 1 will hold a single pool for
 * the services (LoginService, SessionRegistry, ...).</p>
 */
public class ServerApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ServerApp.class);
    private ServerNetworkRuntime networkRuntime;

    @Override
    public void start(Stage stage) throws IOException {
        bootstrapDatabase();
        networkRuntime = new ServerNetworkRuntime();
        networkRuntime.start();
        Label placeholder = new Label("PC Server - scaffold (MVC shell)");
        placeholder.setId("placeholder-label");
        Scene scene = new Scene(placeholder, 800, 600);
        stage.setTitle("DAU Computer Lab Management - Server");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws IOException {
        if (networkRuntime != null) {
            networkRuntime.close();
        }
    }

    /**
     * Applies pending Flyway migrations from db/migration. Skips silently
     * when DB_PASSWORD is not configured; logs an error when the database
     * cannot be reached.
     */
    private void bootstrapDatabase() {
        AppConfig config = AppConfig.fromEnv();
        if (config.dbPassword() == null || config.dbPassword().isBlank()) {
            LOG.warn("DB_PASSWORD is not set; skipping database bootstrap");
            return;
        }
        try (HikariDataSource dataSource = DataSourceProvider.create(config)) {
            int applied = DatabaseMigrator.migrate(dataSource, config.migrationLocations());
            LOG.info("Database ready at {}; Flyway applied {} migration(s)", config.dbUrl(), applied);
        } catch (Exception e) {
            LOG.error("Database bootstrap failed", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
