package com.dau.labmanagement.server.view;

import com.dau.labmanagement.server.model.entity.UserRole;
import com.dau.labmanagement.server.model.service.LoginFailureReason;
import com.dau.labmanagement.server.model.service.LoginResult;
import com.dau.labmanagement.server.model.service.LoginSession;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end UI flow for SC_001: the window opens on the login form and, after a
 * successful authentication, swaps to the dashboard of the returned role.
 */
class ServerNavigatorTest {

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
    void successfulLoginReplacesTheLoginFormWithTheRoleDashboard() throws Exception {
        LoginSession session = new LoginSession(
                UUID.randomUUID(), 5L, "gv01", "Giảng viên Một", UserRole.TEACHER, OffsetDateTime.now());
        Navigator nav = createNavigator();

        nav.onFx(() -> {
            nav.navigator().showLogin(
                    (username, password) -> LoginResult.success(session),
                    s -> nav.navigator().showDashboard(s, () -> { }));
            assertNotNull(nav.scene().lookup("#usernameField"), "login form must be shown first");
            return null;
        });

        nav.onFx(() -> {
            ((TextField) nav.scene().lookup("#usernameField")).setText("gv01");
            ((PasswordField) nav.scene().lookup("#passwordField")).setText("Teacher@123");
            ((Button) nav.scene().lookup("#loginButton")).fire();
            return null;
        });

        nav.onFx(() -> {
            Label identity = (Label) nav.scene().lookup("#identityLabel");
            assertNotNull(identity, "dashboard must replace the login form");
            assertTrue(identity.getText().contains("Giảng viên Một"));
            assertTrue(nav.stage().getTitle().contains("Giảng viên"));
            return null;
        });

        nav.close();
    }

    @Test
    void failedLoginKeepsTheLoginFormWithAnError() throws Exception {
        Navigator nav = createNavigator();

        nav.onFx(() -> {
            nav.navigator().showLogin(
                    (username, password) -> LoginResult.rejected(LoginFailureReason.INVALID_CREDENTIALS),
                    s -> nav.navigator().showDashboard(s, () -> { }));
            ((TextField) nav.scene().lookup("#usernameField")).setText("gv01");
            ((PasswordField) nav.scene().lookup("#passwordField")).setText("bad");
            ((Button) nav.scene().lookup("#loginButton")).fire();
            return null;
        });

        nav.onFx(() -> {
            Label error = (Label) nav.scene().lookup("#errorLabel");
            assertNotNull(nav.scene().lookup("#loginButton"), "login form must stay visible");
            assertEquals(LoginFailureReason.INVALID_CREDENTIALS.message(), error.getText());
            assertTrue(error.isVisible());
            return null;
        });

        nav.close();
    }

    private static Navigator createNavigator() throws Exception {
        AtomicReference<Navigator> reference = new AtomicReference<>();
        CountDownLatch created = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ServerNavigator navigator = new ServerNavigator(stage);
                reference.set(new Navigator(stage, navigator, stage.getScene()));
            } finally {
                created.countDown();
            }
        });
        assertTrue(created.await(10, TimeUnit.SECONDS), "navigator should be created");
        return reference.get();
    }

    private record Navigator(Stage stage, ServerNavigator navigator, javafx.scene.Scene scene) {

        <T> T onFx(java.util.concurrent.Callable<T> action) throws Exception {
            AtomicReference<T> result = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    result.set(action.call());
                } catch (Throwable throwable) {
                    failure.set(throwable);
                } finally {
                    done.countDown();
                }
            });
            assertTrue(done.await(10, TimeUnit.SECONDS), "JavaFX action should complete");
            if (failure.get() != null) {
                throw new AssertionError("JavaFX action failed", failure.get());
            }
            return result.get();
        }

        void close() {
            Platform.runLater(stage::close);
        }
    }
}
