package com.dau.labmanagement.server.testsupport;

import com.dau.labmanagement.server.model.service.JdbiLoginService;
import com.dau.labmanagement.server.model.service.LoginSessionRegistry;
import com.dau.labmanagement.server.view.ServerNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Opens the real SC_001 window against a throw-away embedded PostgreSQL so the
 * login flow can be tried by hand without configuring {@code DB_PASSWORD}.
 *
 * <p>Run with {@code gradlew.bat :server:demoLogin}. Accounts:</p>
 * <ul>
 *   <li>{@code admin / Admin@123} — Admin role</li>
 *   <li>{@code gv01 / Teacher@123} — Giảng viên role</li>
 *   <li>{@code gv_locked / Teacher@123} — locked account</li>
 *   <li>{@code sv01 / Teacher@123} — student, rejected on the Server</li>
 * </ul>
 */
public final class LoginDemoLauncher extends Application {

    private static LoginTestDatabase database;
    private static LoginSessionRegistry sessions;
    private static JdbiLoginService loginService;

    @Override
    public void start(Stage stage) {
        ServerNavigator navigator = new ServerNavigator(stage);
        navigator.showLogin(loginService, session -> navigator.showDashboard(session, () -> {
            sessions.close(session.sessionId());
            navigator.showLogin(loginService, next -> navigator.showDashboard(next, () -> { }));
        }));
    }

    @Override
    public void stop() throws Exception {
        if (sessions != null) {
            sessions.closeAll();
        }
        if (database != null) {
            database.close();
        }
    }

    public static void main(String[] args) throws Exception {
        database = LoginTestDatabase.start();
        sessions = new LoginSessionRegistry();
        loginService = new JdbiLoginService(database.jdbi(), sessions);
        System.out.println("SC_001 demo database ready. Accounts:");
        System.out.println("  admin     / Admin@123   (Admin)");
        System.out.println("  gv01      / Teacher@123 (Giảng viên)");
        System.out.println("  gv_locked / Teacher@123 (bị khóa)");
        System.out.println("  sv01      / Teacher@123 (sinh viên - bị từ chối)");
        launch(args);
    }
}
