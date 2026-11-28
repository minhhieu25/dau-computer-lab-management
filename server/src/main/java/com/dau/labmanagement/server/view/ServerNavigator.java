package com.dau.labmanagement.server.view;

import com.dau.labmanagement.server.ServerApp;
import com.dau.labmanagement.server.controller.DashboardController;
import com.dau.labmanagement.server.controller.LoginController;
import com.dau.labmanagement.server.model.service.LoginService;
import com.dau.labmanagement.server.model.service.LoginSession;
import com.dau.labmanagement.server.model.service.dashboard.MachineSessionRegistry;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * VIEW helper that swaps the Server window between SC_001 (login) and the
 * role-scoped dashboard shell after a successful login.
 */
public final class ServerNavigator {

    private static final String LOGIN_VIEW = "/fxml/login.fxml";
    private static final String DASHBOARD_VIEW = "/fxml/dashboard.fxml";
    private static final String STYLESHEET = "/css/server.css";

    private final Stage stage;
    private final Scene scene;

    /**
     * @param stage the primary Server window
     */
    public ServerNavigator(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.scene = new Scene(new StackPane(), 1000, 720);
        scene.getStylesheets().add(requireResource(STYLESHEET).toExternalForm());
        stage.setMinWidth(880);
        stage.setMinHeight(600);
        stage.setScene(scene);
    }

    /**
     * Shows the login form.
     *
     * @param loginService   service that authenticates teacher/admin accounts
     * @param onLoginSuccess callback receiving the opened session
     */
    public void showLogin(LoginService loginService, Consumer<LoginSession> onLoginSuccess) {
        View<LoginController> view = load(LOGIN_VIEW);
        view.controller().setLoginService(loginService);
        view.controller().setOnLoginSuccess(Objects.requireNonNull(onLoginSuccess, "onLoginSuccess"));
        stage.setTitle("DAU Lab Management - Server · Đăng nhập");
        scene.setRoot(view.root());
        stage.show();
    }

    /**
     * Shows the dashboard shell for an authenticated role.
     *
     * @param session  the authenticated session
     * @param onLogout callback invoked when the user logs out
     */
    public void showDashboard(LoginSession session, Runnable onLogout) {
        showDashboard(session, onLogout, null);
    }

    /**
     * Shows the dashboard shell for an authenticated role.
     *
     * @param session        the authenticated session
     * @param onLogout       callback invoked when the user logs out
     * @param machineRegistry live network state, or null when the network is unavailable
     */
    public void showDashboard(
            LoginSession session,
            Runnable onLogout,
            MachineSessionRegistry machineRegistry) {
        View<DashboardController> view = load(DASHBOARD_VIEW);
        view.controller().setOnLogout(Objects.requireNonNull(onLogout, "onLogout"));
        view.controller().setMachineRegistry(machineRegistry);
        view.controller().setSession(session);
        stage.setTitle("DAU Lab Management - Server · " + session.roleLabel());
        scene.setRoot(view.root());
        stage.show();
    }

    private <T> View<T> load(String resource) {
        URL url = requireResource(resource);
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            return new View<>(root, loader.getController());
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot load view " + resource, exception);
        }
    }

    private static URL requireResource(String resource) {
        URL url = ServerApp.class.getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Missing resource " + resource);
        }
        return url;
    }

    private record View<T>(Parent root, T controller) {
    }
}
