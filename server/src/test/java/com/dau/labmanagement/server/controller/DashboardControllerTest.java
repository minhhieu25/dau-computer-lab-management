package com.dau.labmanagement.server.controller;

import com.dau.labmanagement.server.model.entity.UserRole;
import com.dau.labmanagement.server.model.service.LoginSession;
import com.dau.labmanagement.server.model.service.dashboard.MachineSessionRegistry;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI test for the post-login navigation: the dashboard must expose only the
 * entries allowed for the authenticated role (BR-09).
 */
class DashboardControllerTest {

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
    void teacherSeesOnlyClassroomNavigation() throws Exception {
        Shell shell = loadShell(session(UserRole.TEACHER, "Giảng viên Một"));

        List<String> entries = shell.entries();
        assertTrue(entries.contains("Dashboard phòng máy"));
        assertTrue(entries.contains("Khóa / Mở khóa"));
        assertTrue(entries.contains("Trình chiếu bài giảng"));
        assertFalse(entries.contains("Quản lý tài khoản"), "teacher must not manage accounts");
        assertFalse(entries.contains("Quản lý bản ghi"), "teacher must not manage all recordings");
        assertTrue(shell.identity().getText().contains("Giảng viên"));
    }

    @Test
    void adminSeesOnlyAdministrationNavigation() throws Exception {
        Shell shell = loadShell(session(UserRole.ADMIN, "System Administrator"));

        List<String> entries = shell.entries();
        assertTrue(entries.contains("Quản lý tài khoản"));
        assertTrue(entries.contains("Quản lý bản ghi"));
        assertFalse(entries.contains("Khóa / Mở khóa"), "admin must not control the classroom");
        assertFalse(entries.contains("Trình chiếu bài giảng"), "admin must not control the classroom");
        assertTrue(shell.identity().getText().contains("Admin"));
    }

    @Test
    void logoutInvokesTheCallback() throws Exception {
        Shell shell = loadShell(session(UserRole.TEACHER, "Giảng viên Một"));
        AtomicReference<Boolean> loggedOut = new AtomicReference<>(false);
        shell.controller().setOnLogout(() -> loggedOut.set(true));

        shell.logout().fire();

        assertTrue(loggedOut.get());
        assertTrue(shell.status().getText().contains("Đã đăng xuất"));
    }

    @Test
    void exposesTheActiveSession() throws Exception {
        LoginSession session = session(UserRole.ADMIN, "System Administrator");
        Shell shell = loadShell(session);

        assertEquals(session, shell.controller().getSession());
    }

    @Test
    void firstNavigationEntryIsSelected() throws Exception {
        Shell shell = loadShell(session(UserRole.TEACHER, "Giảng viên Một"));

        assertTrue(shell.pageTitle().getText().contains("Dashboard phòng máy"));
    }

    @Test
    void teacherDashboardUsesTheLiveMachineRegistryWhenAvailable() throws Exception {
        Shell shell = loadShell(
                session(UserRole.TEACHER, "Giảng viên Một"),
                new MachineSessionRegistry());

        assertTrue(shell.contentOutlet().getChildren().get(0) instanceof TeacherDashboardView);
    }

    @Test
    void teacherDashboardExplainsWhenNetworkRuntimeIsUnavailable() throws Exception {
        Shell shell = loadShell(session(UserRole.TEACHER, "Giảng viên Một"));
        VBox panel = (VBox) shell.contentOutlet().getChildren().get(0);

        assertTrue(panel.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .anyMatch(label -> label.getText().contains("network runtime")));
    }

    private static LoginSession session(UserRole role, String fullName) {
        return new LoginSession(UUID.randomUUID(), 1L, "user01", fullName, role, OffsetDateTime.now());
    }

    private static Shell loadShell(LoginSession session) throws Exception {
        return loadShell(session, null);
    }

    private static Shell loadShell(
            LoginSession session,
            MachineSessionRegistry machineRegistry) throws Exception {
        AtomicReference<Shell> reference = new AtomicReference<>();
        CountDownLatch loaded = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        DashboardControllerTest.class.getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();
                DashboardController controller = loader.getController();
                controller.setMachineRegistry(machineRegistry);
                controller.setSession(session);
                reference.set(new Shell(
                        controller,
                        (VBox) root.lookup("#navigation"),
                        (Label) root.lookup("#identityLabel"),
                        (Label) root.lookup("#pageTitle"),
                        (Label) root.lookup("#statusLabel"),
                        (Button) root.lookup("#logoutButton"),
                        (StackPane) root.lookup("#contentOutlet")));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            } finally {
                loaded.countDown();
            }
        });
        assertTrue(loaded.await(10, TimeUnit.SECONDS), "FXML should load on the JavaFX thread");
        return reference.get();
    }

    private record Shell(
            DashboardController controller,
            VBox navigation,
            Label identity,
            Label pageTitle,
            Label status,
            Button logout,
            StackPane contentOutlet) {

        List<String> entries() {
            List<String> titles = new ArrayList<>();
            navigation.getChildren().forEach(node -> titles.add(((Button) node).getText()));
            return titles;
        }
    }
}
