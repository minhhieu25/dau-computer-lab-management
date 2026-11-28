package com.dau.labmanagement.client.controller;

import com.dau.labmanagement.shared.protocol.message.LoginAckMessage;
import com.dau.labmanagement.shared.protocol.message.LoginStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientShellControllerTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit should start");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void showsTheAuthenticatedStudentIdentity() throws Exception {
        assertLoginPresentation(
                new LoginAckMessage(
                        LoginStatus.AUTHENTICATED, null, null, 0,
                        42L, "2101207", "Minh Khoa", null),
                "Định danh thành công.",
                "Minh Khoa · MSSV 2101207");
    }

    @Test
    void showsTheServerAuthenticationError() throws Exception {
        assertLoginPresentation(
                new LoginAckMessage(
                        LoginStatus.REJECTED, null, null, 0,
                        null, null, null, "Sai MSSV/mật khẩu hoặc tài khoản đã bị khóa."),
                "Sai MSSV/mật khẩu hoặc tài khoản đã bị khóa.",
                "");
    }

    private static void assertLoginPresentation(
            LoginAckMessage result, String expectedFeedback, String expectedIdentity)
            throws Exception {
        AtomicReference<Parent> rootReference = new AtomicReference<>();
        AtomicReference<ClientShellController> controllerReference = new AtomicReference<>();
        callOnFx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    ClientShellControllerTest.class.getResource("/fxml/client-shell.fxml"));
            Parent root = loader.load();
            rootReference.set(root);
            controllerReference.set(loader.getController());
            return null;
        });

        Parent root = rootReference.get();
        CountDownLatch feedbackUpdated = new CountDownLatch(1);
        CountDownLatch loginInvoked = new CountDownLatch(1);
        AtomicReference<Label> feedbackReference = new AtomicReference<>();
        callOnFx(() -> {
            ClientShellController controller = controllerReference.get();
            controller.setLoginAction((studentId, password) -> {
                loginInvoked.countDown();
                return result;
            });
            Label feedback = findLabels(root).stream()
                    .filter(label -> label.getStyleClass().contains("login-feedback"))
                    .findFirst()
                    .orElseThrow();
            feedbackReference.set(feedback);
            feedback.textProperty().addListener((observable, oldValue, newValue) -> {
                if (expectedFeedback.equals(newValue)) {
                    feedbackUpdated.countDown();
                }
            });

            findTextFields(root).stream()
                    .filter(field -> !(field instanceof PasswordField))
                    .findFirst()
                    .orElseThrow()
                    .setText("2101207");
            findPasswordFields(root).get(0).setText("test-password");
            findButtons(root).stream()
                    .filter(button -> "Đăng nhập".equals(button.getText())
                            && button.getStyleClass().contains("primary-button"))
                    .findFirst()
                    .orElseThrow()
                    .fire();
            return null;
        });

        assertTrue(loginInvoked.await(5, TimeUnit.SECONDS), "Login action should run");
        assertTrue(feedbackUpdated.await(5, TimeUnit.SECONDS),
                "Client should show the authentication result; current feedback: "
                        + callOnFx(() -> feedbackReference.get().getText()));
        assertEquals(expectedFeedback, callOnFx(() -> feedbackReference.get().getText()));
        assertEquals(expectedIdentity, callOnFx(() -> findLabels(root).stream()
                .filter(label -> label.getStyleClass().contains("login-identity"))
                .findFirst()
                .orElseThrow()
                .getText()));
        assertEquals("", callOnFx(() -> findPasswordFields(root).get(0).getText()));
    }

    private static <T> T callOnFx(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static List<TextField> findTextFields(Parent root) {
        return findNodes(root, TextField.class);
    }

    private static List<PasswordField> findPasswordFields(Parent root) {
        return findNodes(root, PasswordField.class);
    }

    private static List<Button> findButtons(Parent root) {
        return findNodes(root, Button.class);
    }

    private static List<Label> findLabels(Parent root) {
        return findNodes(root, Label.class);
    }

    private static <T extends Node> List<T> findNodes(Parent root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collectNodes(root, type, matches);
        return matches;
    }

    private static <T extends Node> void collectNodes(Parent parent, Class<T> type, List<T> matches) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) {
                matches.add(type.cast(child));
            }
            if (child instanceof Parent nestedParent) {
                collectNodes(nestedParent, type, matches);
            }
        }
    }
}
