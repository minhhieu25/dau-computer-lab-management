package com.dau.labmanagement.server.controller;

import com.dau.labmanagement.server.model.service.JdbiLoginService;
import com.dau.labmanagement.server.model.service.LoginSessionRegistry;
import com.dau.labmanagement.server.testsupport.FxTestSupport;
import com.dau.labmanagement.server.testsupport.LoginTestDatabase;
import com.dau.labmanagement.server.view.ServerNavigator;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end SC_001 test: real PostgreSQL (Zonky) + real Flyway migrations +
 * real PBKDF2 hashes + real FXML screens. This is the closest automated
 * equivalent of a person typing credentials into the Server window.
 */
class LoginFlowUiIT {

    private static LoginTestDatabase database;
    private static LoginSessionRegistry sessions;
    private static JdbiLoginService loginService;

    @BeforeAll
    static void startBackend() throws Exception {
        FxTestSupport.ensureStarted();
        database = LoginTestDatabase.start();
        sessions = new LoginSessionRegistry();
        loginService = new JdbiLoginService(database.jdbi(), sessions);
    }

    @AfterAll
    static void stopBackend() throws Exception {
        if (sessions != null) {
            sessions.closeAll();
        }
        if (database != null) {
            database.close();
        }
    }

    @org.junit.jupiter.api.AfterEach
    void closeLeakedSessions() {
        sessions.closeAll();
    }

    @Test
    void teacherLogsInSeesClassroomMenuAndCanLogOut() throws Exception {
        Window window = openWindow();
        try {
            window.login("gv01", LoginTestDatabase.TEACHER_PASSWORD);

            assertEquals("Nguyễn Văn Giảng · Giảng viên", window.text("#identityLabel"));
            assertEquals(List.of(
                            "Dashboard phòng máy", "Giám sát màn hình", "Khóa / Mở khóa",
                            "Trình chiếu bài giảng", "Gửi tài liệu", "Bản ghi của tôi"),
                    window.navigationEntries());
            assertTrue(window.stageTitle().contains("Giảng viên"));
            assertEquals(1, sessions.activeCount(), "one Server session must be open");

            window.logout();

            assertNotNull(window.node("#usernameField"), "logout must return to the login form");
            assertEquals(0, sessions.activeCount(), "logout must close the session");
        } finally {
            window.close();
        }
    }

    @Test
    void adminLogsInAndSeesOnlyAdministrationMenu() throws Exception {
        Window window = openWindow();
        try {
            window.login("admin", LoginTestDatabase.ADMIN_PASSWORD);

            assertEquals("System Administrator · Admin", window.text("#identityLabel"));
            assertEquals(List.of("Tổng quan quản trị", "Quản lý tài khoản", "Quản lý bản ghi"),
                    window.navigationEntries());
            assertTrue(window.stageTitle().contains("Admin"));
        } finally {
            window.close();
        }
    }

    @Test
    void wrongPasswordShowsTheExactMessageAndStaysOnTheForm() throws Exception {
        Window window = openWindow();
        try {
            window.login("gv01", "sai-mat-khau");

            assertNotNull(window.node("#loginButton"), "must stay on the login form");
            assertEquals("Tài khoản hoặc mật khẩu không đúng.", window.text("#errorLabel"));
            assertTrue(window.node("#errorLabel").isVisible());
            assertEquals("", window.password(), "password field must be cleared");
            assertEquals(0, sessions.activeCount());
        } finally {
            window.close();
        }
    }

    @Test
    void lockedAccountIsRejectedWithItsOwnMessage() throws Exception {
        Window window = openWindow();
        try {
            window.login("gv_locked", LoginTestDatabase.TEACHER_PASSWORD);

            assertEquals("Tài khoản đã bị khóa. Liên hệ Admin để mở khóa.", window.text("#errorLabel"));
            assertEquals(0, sessions.activeCount());
        } finally {
            window.close();
        }
    }

    @Test
    void studentCannotLogInToTheServer() throws Exception {
        Window window = openWindow();
        try {
            window.login("sv01", LoginTestDatabase.TEACHER_PASSWORD);

            assertEquals("Tài khoản Sinh viên không được đăng nhập phần mềm Server.",
                    window.text("#errorLabel"));
            assertEquals(0, sessions.activeCount());
        } finally {
            window.close();
        }
    }

    @Test
    void unknownAccountIsRejectedWithoutRevealingThatItDoesNotExist() throws Exception {
        Window window = openWindow();
        try {
            window.login("khong_ton_tai", "bat-ky-mat-khau");

            assertEquals("Tài khoản hoặc mật khẩu không đúng.", window.text("#errorLabel"));
            assertEquals(0, sessions.activeCount());
        } finally {
            window.close();
        }
    }

    @Test
    void emptyFormIsRejectedBeforeTouchingTheDatabase() throws Exception {
        Window window = openWindow();
        try {
            window.login("", "");

            assertEquals("Vui lòng nhập tài khoản và mật khẩu.", window.text("#errorLabel"));
            assertEquals(0, sessions.activeCount());
        } finally {
            window.close();
        }
    }

    private static Window openWindow() {
        return FxTestSupport.call(() -> {
            Stage stage = new Stage();
            ServerNavigator navigator = new ServerNavigator(stage);
            navigator.showLogin(loginService, session -> navigator.showDashboard(session, () -> {
                sessions.close(session.sessionId());
                navigator.showLogin(loginService, s -> navigator.showDashboard(s, () -> { }));
            }));
            return new Window(stage, navigator);
        });
    }

    private record Window(Stage stage, ServerNavigator navigator) {

        Scene scene() {
            return stage.getScene();
        }

        void login(String username, String password) {
            FxTestSupport.run(() -> {
                ((TextField) scene().lookup("#usernameField")).setText(username);
                ((PasswordField) scene().lookup("#passwordField")).setText(password);
                ((Button) scene().lookup("#loginButton")).fire();
            });
        }

        void logout() {
            FxTestSupport.run(() -> ((Button) scene().lookup("#logoutButton")).fire());
        }

        javafx.scene.Node node(String selector) {
            return FxTestSupport.call(() -> scene().lookup(selector));
        }

        String text(String selector) {
            javafx.scene.Node node = node(selector);
            assertNotNull(node, "missing node " + selector);
            return ((Label) node).getText();
        }

        String password() {
            return FxTestSupport.call(() -> ((PasswordField) scene().lookup("#passwordField")).getText());
        }

        String stageTitle() {
            return FxTestSupport.call(stage::getTitle);
        }

        List<String> navigationEntries() {
            return FxTestSupport.call(() -> {
                VBox navigation = (VBox) scene().lookup("#navigation");
                assertNotNull(navigation, "dashboard navigation must be present");
                List<String> titles = new ArrayList<>();
                navigation.getChildren().forEach(node -> titles.add(((Button) node).getText()));
                return titles;
            });
        }

        void close() {
            FxTestSupport.run(stage::close);
        }
    }
}
