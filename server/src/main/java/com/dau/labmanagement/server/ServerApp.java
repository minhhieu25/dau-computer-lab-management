package com.dau.labmanagement.server;

import com.dau.labmanagement.server.config.AppConfig;
import com.dau.labmanagement.server.config.DataSourceProvider;
import com.dau.labmanagement.server.config.DatabaseMigrator;
import com.dau.labmanagement.server.config.JdbiProvider;
import com.dau.labmanagement.server.model.service.JdbiLoginService;
import com.dau.labmanagement.server.model.service.LoginService;
import com.dau.labmanagement.server.model.service.LoginSession;
import com.dau.labmanagement.server.model.service.LoginSessionRegistry;
import com.dau.labmanagement.server.network.ServerNetworkRuntime;
import com.dau.labmanagement.server.view.ServerNavigator;
import com.zaxxer.hikari.HikariDataSource;
import javafx.application.Application;
import javafx.stage.Stage;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Entry point of the PC Server (teacher/admin desktop application).
 *
 * <p>Startup order: database (Flyway migrations + JDBI), network runtime, then
 * the SC_001 login window. The database is optional for the UI: when
 * {@code DB_PASSWORD} is not set the window still opens and every login attempt
 * is rejected with a configuration error instead of crashing the app.</p>
 */
public final class ServerApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ServerApp.class);

    private HikariDataSource dataSource;
    private Jdbi jdbi;
    private ServerNetworkRuntime networkRuntime;
    private LoginSessionRegistry loginSessions;
    private LoginService loginService = LoginService.unavailable();
    private ServerNavigator navigator;
    private LoginSession currentSession;

    @Override
    public void start(Stage stage) {
        loginSessions = new LoginSessionRegistry();
        loginService = bootstrapDatabase();

        networkRuntime = new ServerNetworkRuntime();
        try {
            networkRuntime.start();
        } catch (IOException | RuntimeException exception) {
            LOG.error("Network runtime failed to start; login stays available", exception);
            networkRuntime = null;
        }

        navigator = new ServerNavigator(stage);
        navigator.showLogin(loginService, this::onLoginSuccess);
    }

    @Override
    public void stop() throws IOException {
        if (loginSessions != null) {
            loginSessions.closeAll();
        }
        IOException closeFailure = null;
        if (networkRuntime != null) {
            try {
                networkRuntime.close();
            } catch (IOException exception) {
                closeFailure = exception;
            }
        }
        if (dataSource != null) {
            dataSource.close();
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    /**
     * Applies migrations and builds the login service.
     *
     * @return a database-backed service, or a rejecting one when the database
     *         is not configured or cannot be reached
     */
    private LoginService bootstrapDatabase() {
        AppConfig config = AppConfig.fromEnv();
        if (config.dbPassword() == null || config.dbPassword().isBlank()) {
            LOG.warn("DB_PASSWORD is not set; Server login is unavailable");
            return LoginService.unavailable();
        }
        try {
            dataSource = DataSourceProvider.create(config);
            int applied = DatabaseMigrator.migrate(dataSource, config.migrationLocations());
            jdbi = JdbiProvider.create(dataSource);
            LOG.info("Database ready at {}; Flyway applied {} migration(s)", config.dbUrl(), applied);
            return new JdbiLoginService(jdbi, loginSessions);
        } catch (RuntimeException exception) {
            LOG.error("Database bootstrap failed; Server login is unavailable", exception);
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
            jdbi = null;
            return LoginService.unavailable();
        }
    }

    private void onLoginSuccess(LoginSession session) {
        currentSession = session;
        navigator.showDashboard(session, this::logout);
    }

    private void logout() {
        if (currentSession != null) {
            loginSessions.close(currentSession.sessionId());
            LOG.info("Closed Server login session {} for {}",
                    currentSession.sessionId(), currentSession.username());
            currentSession = null;
        }
        navigator.showLogin(loginService, this::onLoginSuccess);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
