package com.dau.labmanagement.server.testsupport;

import com.dau.labmanagement.server.model.entity.UserRole;
import com.dau.labmanagement.server.model.service.LoginSession;
import com.dau.labmanagement.server.view.ServerNavigator;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Renders the real SC_001 screens to PNG files so the UI can be inspected
 * without a person typing into the window.
 *
 * <p>Run with {@code gradlew.bat :server:captureScreens}; images land in
 * {@code build/screenshots}. No database is needed: the dashboard screenshots
 * use {@link LoginSession} values directly.</p>
 */
public final class ScreenCaptureTool {

    private static final File OUTPUT_DIR = new File("build/screenshots");

    private ScreenCaptureTool() {
    }

    public static void main(String[] args) throws Exception {
        if (!OUTPUT_DIR.exists() && !OUTPUT_DIR.mkdirs()) {
            throw new IllegalStateException("Cannot create " + OUTPUT_DIR.getAbsolutePath());
        }

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        if (!started.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX did not start");
        }

        runAndWait(() -> {
            try {
                Stage stage = new Stage();
                ServerNavigator navigator = new ServerNavigator(stage);
                navigator.showLogin((username, password) -> null, session -> { });
                stage.show();
                capture(stage.getScene(), "01-login.png");

                captureLoginError(stage, navigator);
                capture(stage.getScene(), "02-login-error.png");

                navigator.showDashboard(
                        session("gv01", "Nguyễn Văn Giảng", UserRole.TEACHER), () -> { });
                capture(stage.getScene(), "03-teacher-dashboard.png");

                navigator.showDashboard(
                        session("admin", "System Administrator", UserRole.ADMIN), () -> { });
                capture(stage.getScene(), "04-admin-dashboard.png");

                stage.close();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });

        Platform.exit();
        System.out.println("Screenshots written to " + OUTPUT_DIR.getAbsolutePath());
    }

    private static LoginSession session(String username, String fullName, UserRole role) {
        return new LoginSession(
                UUID.randomUUID(), 1L, username, fullName, role, OffsetDateTime.now());
    }

    private static void captureLoginError(Stage stage, ServerNavigator navigator) {
        navigator.showLogin(
                (username, password) -> com.dau.labmanagement.server.model.service.LoginResult.rejected(
                        com.dau.labmanagement.server.model.service.LoginFailureReason.INVALID_CREDENTIALS),
                session -> { });
        ((javafx.scene.control.TextField) stage.getScene().lookup("#usernameField")).setText("gv01");
        ((javafx.scene.control.PasswordField) stage.getScene().lookup("#passwordField"))
                .setText("sai-mat-khau");
        ((javafx.scene.control.Button) stage.getScene().lookup("#loginButton")).fire();
    }

    private static void capture(Scene scene, String fileName) throws Exception {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        WritableImage image = scene.snapshot(null);
        File file = new File(OUTPUT_DIR, fileName);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        System.out.println("Wrote " + file.getAbsolutePath());
    }

    private static void runAndWait(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                done.countDown();
            }
        });
        if (!done.await(120, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Capture did not finish");
        }
    }
}
