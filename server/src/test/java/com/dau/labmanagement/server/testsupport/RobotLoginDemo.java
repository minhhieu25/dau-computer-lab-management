package com.dau.labmanagement.server.testsupport;

import com.dau.labmanagement.server.model.service.JdbiLoginService;
import com.dau.labmanagement.server.model.service.LoginSessionRegistry;
import com.dau.labmanagement.server.view.ServerNavigator;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Drives the real SC_001 window with mouse events (JavaFX Robot) and captures a
 * screenshot after every step.
 *
 * <p>Run with {@code gradlew.bat :server:robotLogin}; images land in
 * {@code build/screenshots/robot}. Uses a throw-away embedded PostgreSQL with
 * the demo accounts, so no local database setup is required.</p>
 *
 * <p>Keyboard events sent by {@link Robot} are not delivered reliably when the
 * window is not the foreground window of the desktop session, so the demo
 * clicks the buttons with the mouse and, if an OS-level click is dropped,
 * falls back to firing the button directly. Both paths exercise the same
 * {@code onAction} handler.</p>
 */
public final class RobotLoginDemo {

    private static final File OUTPUT_DIR = new File("build/screenshots/robot");
    private static final String TEACHER = "gv01";
    private static final String TEACHER_PASSWORD = LoginTestDatabase.TEACHER_PASSWORD;
    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = LoginTestDatabase.ADMIN_PASSWORD;

    private RobotLoginDemo() {
    }

    public static void main(String[] args) throws Exception {
        if (!OUTPUT_DIR.exists() && !OUTPUT_DIR.mkdirs()) {
            throw new IllegalStateException("Cannot create " + OUTPUT_DIR.getAbsolutePath());
        }

        LoginTestDatabase database = LoginTestDatabase.start();
        LoginSessionRegistry sessions = new LoginSessionRegistry();
        JdbiLoginService loginService = new JdbiLoginService(database.jdbi(), sessions);
        try {
            startToolkit();
            runSteps(loginService, sessions);
        } finally {
            sessions.closeAll();
            database.close();
            Platform.exit();
        }
        System.out.println("Robot screenshots written to " + OUTPUT_DIR.getAbsolutePath());
    }

    private static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        if (!started.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX did not start");
        }
    }

    private static void runSteps(JdbiLoginService loginService, LoginSessionRegistry sessions)
            throws Exception {
        Stage stage = call(() -> {
            Stage created = new Stage();
            ServerNavigator navigator = new ServerNavigator(created);
            navigator.showLogin(loginService, session -> navigator.showDashboard(session, () -> {
                sessions.close(session.sessionId());
                navigator.showLogin(loginService, next -> navigator.showDashboard(next, () -> { }));
            }));
            created.show();
            created.toFront();
            created.requestFocus();
            return created;
        });

        pause(1200);
        shoot(stage.getScene(), "01-login.png");

        typeCredentials(stage, TEACHER, TEACHER_PASSWORD);
        requireStep(submitAndAwait(stage, scene -> scene.lookup("#logoutButton") != null),
                "teacher " + TEACHER + " reached the dashboard");
        pause(800);
        shoot(stage.getScene(), "02-teacher-dashboard.png");

        clickNode(stage, "#logoutButton");
        requireStep(awaitNode(stage, "#usernameField", 5000), "logout returned to the login form");
        pause(600);
        shoot(stage.getScene(), "03-after-logout.png");

        typeCredentials(stage, TEACHER, "sai-mat-khau");
        requireStep(submitAndAwait(stage, RobotLoginDemo::errorVisible),
                "wrong password shows the error message");
        pause(600);
        shoot(stage.getScene(), "04-wrong-password.png");
        System.out.println("Error shown: " + text(stage, "#errorLabel"));

        clearCredentials(stage);
        typeCredentials(stage, ADMIN, ADMIN_PASSWORD);
        requireStep(submitAndAwait(stage, scene -> scene.lookup("#logoutButton") != null),
                "admin " + ADMIN + " reached the admin dashboard");
        pause(800);
        shoot(stage.getScene(), "05-admin-dashboard.png");
        System.out.println("Admin title: " + text(stage, "#pageTitle"));

        call(() -> {
            stage.close();
            return null;
        });
    }

    private static void requireStep(boolean ok, String description) {
        if (!ok) {
            throw new IllegalStateException("Step failed: " + description);
        }
        System.out.println("OK: " + description);
    }

    private static void typeCredentials(Stage stage, String username, String password) throws Exception {
        call(() -> {
            TextField userField = (TextField) stage.getScene().lookup("#usernameField");
            PasswordField passwordField = (PasswordField) stage.getScene().lookup("#passwordField");
            userField.requestFocus();
            userField.clear();
            userField.setText(username);
            passwordField.clear();
            passwordField.setText(password);
            passwordField.requestFocus();
            return null;
        });
    }

    private static void clearCredentials(Stage stage) throws Exception {
        call(() -> {
            ((TextField) stage.getScene().lookup("#usernameField")).clear();
            ((PasswordField) stage.getScene().lookup("#passwordField")).clear();
            return null;
        });
    }

    /** Clicks #loginButton, waits for the expected state, retrying with a direct fire if needed. */
    private static boolean submitAndAwait(Stage stage, java.util.function.Predicate<Scene> reached)
            throws Exception {
        clickNode(stage, "#loginButton");
        if (awaitCondition(stage, reached, 4000)) {
            return true;
        }
        System.out.println("Robot click was not delivered; firing #loginButton directly");
        call(() -> {
            ((Button) stage.getScene().lookup("#loginButton")).fire();
            return null;
        });
        return awaitCondition(stage, reached, 10000);
    }

    private static boolean errorVisible(Scene scene) {
        Node node = scene.lookup("#errorLabel");
        return node instanceof Label label && label.isVisible() && !label.getText().isBlank();
    }

    private static void clickNode(Stage stage, String selector) throws Exception {
        call(() -> {
            Node target = stage.getScene().lookup(selector);
            if (target == null) {
                throw new IllegalStateException("Missing node " + selector);
            }
            Bounds bounds = target.localToScreen(target.getBoundsInLocal());
            if (bounds == null) {
                throw new IllegalStateException("Node " + selector + " is not on screen");
            }
            Robot robot = new Robot();
            robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
            robot.mouseClick(MouseButton.PRIMARY);
            return null;
        });
    }

    private static boolean awaitNode(Stage stage, String selector, long timeoutMillis) throws Exception {
        return awaitCondition(stage, scene -> scene.lookup(selector) != null, timeoutMillis);
    }

    private static boolean awaitCondition(Stage stage, java.util.function.Predicate<Scene> condition,
                                          long timeoutMillis) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            if (call(() -> condition.test(stage.getScene()))) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private static String text(Stage stage, String selector) throws Exception {
        return call(() -> {
            Node node = stage.getScene().lookup(selector);
            return node instanceof Label label ? label.getText() : String.valueOf(node);
        });
    }

    private static void shoot(Scene scene, String fileName) throws Exception {
        call(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            WritableImage image = scene.snapshot(null);
            File file = new File(OUTPUT_DIR, fileName);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot write " + file, exception);
            }
            System.out.println("Wrote " + file.getName());
            return null;
        });
    }

    private static void pause(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private static <T> T call(java.util.concurrent.Callable<T> action) throws Exception {
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
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
        if (!done.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX action did not finish");
        }
        if (failure.get() != null) {
            throw new IllegalStateException("JavaFX action failed", failure.get());
        }
        return result.get();
    }
}
