package com.dau.labmanagement.server.controller;

import com.dau.labmanagement.server.model.entity.UserRole;
import com.dau.labmanagement.server.model.service.LoginFailureReason;
import com.dau.labmanagement.server.model.service.LoginResult;
import com.dau.labmanagement.server.model.service.LoginSession;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI test for SC_001: loads the real FXML, drives the form and asserts the
 * error text and the role-based navigation callback.
 */
class LoginControllerTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.countDown();
            });
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit should start");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.setImplicitExit(true);
    }

    @Test
    void successfulTeacherLoginNavigatesWithTheSession() throws Exception {
        LoginSession session = session(UserRole.TEACHER, "Giảng viên Một");
        Form form = loadForm();

        form.setService((username, password) -> LoginResult.success(session));
        form.username().setText("gv01");
        form.password().setText("Teacher@123");
        form.button().fire();

        assertEquals(session, form.successReference().get());
        assertFalse(form.error().isVisible());
        assertTrue(form.status().getText().contains("Giảng viên"));
        assertEquals("", form.password().getText(), "password field must be cleared");
    }

    @Test
    void successfulAdminLoginNavigatesToTheAdminDashboard() throws Exception {
        LoginSession session = session(UserRole.ADMIN, "System Administrator");
        Form form = loadForm();

        form.setService((username, password) -> LoginResult.success(session));
        form.username().setText("admin");
        form.password().setText("Admin@123");
        form.button().fire();

        assertEquals(session, form.successReference().get());
        assertEquals(UserRole.ADMIN, form.successReference().get().role());
    }

    @Test
    void wrongPasswordShowsTheServerMessage() throws Exception {
        Form form = loadForm();

        form.setService((username, password) ->
                LoginResult.rejected(LoginFailureReason.INVALID_CREDENTIALS));
        form.username().setText("gv01");
        form.password().setText("sai-mat-khau");
        form.button().fire();

        assertTrue(form.error().isVisible());
        assertEquals("Tài khoản hoặc mật khẩu không đúng.", form.error().getText());
        assertNull(form.successReference().get());
        assertEquals("", form.password().getText());
    }

    @Test
    void lockedAccountShowsTheLockedMessage() throws Exception {
        Form form = loadForm();

        form.setService((username, password) ->
                LoginResult.rejected(LoginFailureReason.ACCOUNT_LOCKED));
        form.username().setText("gv_locked");
        form.password().setText("Teacher@123");
        form.button().fire();

        assertTrue(form.error().getText().contains("bị khóa"));
    }

    @Test
    void emptyPasswordIsRejectedWithoutCallingTheService() throws Exception {
        List<String> calls = new ArrayList<>();
        Form form = loadForm();

        form.setService((username, password) -> {
            calls.add(username);
            return LoginResult.rejected(LoginFailureReason.SYSTEM_UNAVAILABLE);
        });
        form.username().setText("gv01");
        form.button().fire();

        assertTrue(form.error().isVisible());
        assertEquals(LoginFailureReason.EMPTY_INPUT.message(), form.error().getText());
        assertTrue(calls.isEmpty(), "service must not be called for empty input");
    }

    @Test
    void serviceFailureIsReportedAsUnavailable() throws Exception {
        Form form = loadForm();

        form.setService((username, password) -> {
            throw new IllegalStateException("database down");
        });
        form.username().setText("gv01");
        form.password().setText("Teacher@123");
        form.button().fire();

        assertTrue(form.error().isVisible());
        assertEquals(LoginFailureReason.SYSTEM_UNAVAILABLE.message(), form.error().getText());
    }

    @Test
    void errorIsHiddenAgainOnTheNextAttempt() throws Exception {
        Form form = loadForm();
        form.setService((username, password) ->
                LoginResult.rejected(LoginFailureReason.INVALID_CREDENTIALS));

        form.username().setText("gv01");
        form.password().setText("bad");
        form.button().fire();
        assertTrue(form.error().isVisible());

        form.setService((username, password) ->
                LoginResult.success(session(UserRole.TEACHER, "Giảng viên Một")));
        form.password().setText("Teacher@123");
        form.button().fire();

        assertFalse(form.error().isVisible());
    }

    @Test
    void stylesheetAppliesToTheLoginView() throws Exception {
        Form form = loadForm();
        CountDownLatch applied = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                javafx.scene.Scene scene = new javafx.scene.Scene(form.root(), 1000, 720);
                scene.getStylesheets().add(
                        LoginControllerTest.class.getResource("/css/server.css").toExternalForm());
                form.root().applyCss();
                assertFalse(form.button().getStyleClass().isEmpty());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                applied.countDown();
            }
        });
        assertTrue(applied.await(10, TimeUnit.SECONDS), "CSS should be applied");
        if (failure.get() != null) {
            throw new AssertionError("Stylesheet failed to load", failure.get());
        }
    }

    private static LoginSession session(UserRole role, String fullName) {
        return new LoginSession(UUID.randomUUID(), 1L, "gv01", fullName, role, OffsetDateTime.now());
    }

    private static Form loadForm() throws Exception {
        AtomicReference<Form> reference = new AtomicReference<>();
        CountDownLatch loaded = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(LoginControllerTest.class.getResource("/fxml/login.fxml"));
                Parent root = loader.load();
                LoginController controller = loader.getController();
                reference.set(new Form(
                        controller,
                        root,
                        (TextField) root.lookup("#usernameField"),
                        (PasswordField) root.lookup("#passwordField"),
                        (Button) root.lookup("#loginButton"),
                        (Label) root.lookup("#errorLabel"),
                        (Label) root.lookup("#statusLabel"),
                        new AtomicReference<>()));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            } finally {
                loaded.countDown();
            }
        });
        assertTrue(loaded.await(10, TimeUnit.SECONDS), "FXML should load on the JavaFX thread");
        return reference.get();
    }

    private record Form(
            LoginController controller,
            Parent root,
            TextField username,
            PasswordField password,
            Button button,
            Label error,
            Label status,
            AtomicReference<LoginSession> successReference) {

        void setService(com.dau.labmanagement.server.model.service.LoginService service) {
            controller.setLoginService(service);
            controller.setOnLoginSuccess(successReference::set);
        }
    }
}
