package com.dau.labmanagement.client.controller;

import com.dau.labmanagement.shared.protocol.message.LoginAckMessage;
import com.dau.labmanagement.shared.protocol.message.LoginStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

/** JavaFX shell for Client student identity login. */
public final class ClientShellController {
    @FXML private StackPane contentOutlet;
    @FXML private Label pageTitle;
    @FXML private Label clientStatus;
    @FXML private Button loginNav;
    @FXML private Button homeNav;

    private TextField studentIdInput;
    private PasswordField passwordInput;
    private Button loginButton;
    private Label feedback;
    private Label identity;
    private Node loginView;
    private LoginAction loginAction;
    private String authenticatedStudentId;
    private String authenticatedFullName;

    @FXML
    private void initialize() {
        loginView = createLoginView();
        showLogin();
    }

    @FXML
    private void showLogin() {
        select(loginNav);
        pageTitle.setText("Đăng nhập định danh Client");
        contentOutlet.getChildren().setAll(loginView);
    }

    @FXML
    private void showIdentity() {
        select(homeNav);
        pageTitle.setText("Định danh sinh viên");
        VBox panel = new VBox(12);
        panel.getStyleClass().add("surface");
        panel.setMaxWidth(420);
        Label heading = new Label("Trạng thái định danh");
        heading.getStyleClass().add("section-title");
        Label status = new Label(authenticatedStudentId == null
                ? "Chưa đăng nhập · khi đã kết nối Server, máy vẫn Online."
                : "Đã định danh: " + authenticatedFullName + " · " + authenticatedStudentId);
        status.getStyleClass().add("body-text");
        panel.getChildren().addAll(heading, status);
        contentOutlet.getChildren().setAll(panel);
    }

    private Node createLoginView() {
        studentIdInput = new TextField();
        studentIdInput.setPromptText("Mã sinh viên (MSSV)");
        passwordInput = new PasswordField();
        passwordInput.setPromptText("Mật khẩu");
        loginButton = new Button("Đăng nhập");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> submitLogin());
        feedback = new Label("Chưa đăng nhập");
        feedback.getStyleClass().addAll("muted", "login-feedback");
        identity = new Label();
        identity.getStyleClass().add("login-identity");
        identity.setWrapText(true);

        VBox form = new VBox(10);
        form.getStyleClass().add("surface");
        form.setMaxWidth(390);
        Label icon = new Label("▣");
        icon.getStyleClass().add("login-icon");
        Label heading = new Label("Định danh sinh viên");
        heading.getStyleClass().add("login-heading");
        Label description = new Label(
                "Đăng nhập trên máy Client để hệ thống nhận biết máy của sinh viên.");
        description.getStyleClass().add("muted");
        description.setWrapText(true);
        Label studentIdLabel = new Label("Mã sinh viên (MSSV)");
        studentIdLabel.getStyleClass().add("field-label");
        Label passwordLabel = new Label("Mật khẩu");
        passwordLabel.getStyleClass().add("field-label");
        form.getChildren().addAll(
                icon, heading, description, studentIdLabel, studentIdInput,
                passwordLabel, passwordInput, loginButton, feedback, identity);

        VBox note = new VBox(10);
        note.getStyleClass().addAll("surface", "login-note");
        note.setMaxWidth(330);
        Label noteHeading = new Label("Nguyên tắc định danh");
        noteHeading.getStyleClass().add("section-title");
        Label online = new Label(
                "Chưa đăng nhập · khi đã kết nối Server, máy vẫn Online và nhận lệnh.");
        online.getStyleClass().add("body-text");
        online.setWrapText(true);
        Label privacy = new Label(
                "Mật khẩu không được gửi nguyên văn. Đăng nhập chỉ gắn danh tính sinh viên "
                        + "với máy và phiên hiện tại.");
        privacy.getStyleClass().add("muted");
        privacy.setWrapText(true);
        note.getChildren().addAll(noteHeading, online, privacy);

        HBox layout = new HBox(18, form, note);
        layout.getStyleClass().add("login-layout");
        layout.setMaxWidth(760);
        return layout;
    }

    private void submitLogin() {
        String studentId = studentIdInput.getText() == null
                ? "" : studentIdInput.getText().trim();
        char[] password = passwordInput.getText().toCharArray();
        if (studentId.isBlank() || studentId.length() > 50 || password.length == 0) {
            Arrays.fill(password, '\0');
            showFailure("Vui lòng nhập MSSV hợp lệ và mật khẩu.");
            return;
        }
        if (loginAction == null) {
            Arrays.fill(password, '\0');
            showFailure("Chưa kết nối Server. Máy chỉ Online khi kết nối được thiết lập.");
            return;
        }

        loginButton.setDisable(true);
        feedback.setText("Đang xác thực với Server...");
        feedback.getStyleClass().removeAll("login-error", "login-success");
        feedback.getStyleClass().add("muted");
        Thread.ofVirtual().start(() -> {
            try {
                LoginAckMessage result = loginAction.login(studentId, password);
                Platform.runLater(() -> showLoginResult(result));
            } catch (IOException exception) {
                Platform.runLater(() -> showFailure(
                        "Không thể hoàn tất xác thực với Server. Vui lòng thử lại."));
            } finally {
                Arrays.fill(password, '\0');
            }
        });
    }

    private void showLoginResult(LoginAckMessage result) {
        if (result != null && result.status() == LoginStatus.AUTHENTICATED
                && result.studentId() != null && result.fullName() != null) {
            authenticatedStudentId = result.studentId();
            authenticatedFullName = result.fullName();
            feedback.setText("Định danh thành công.");
            feedback.getStyleClass().removeAll("muted", "login-error");
            feedback.getStyleClass().add("login-success");
            identity.setText(authenticatedFullName + " · MSSV " + authenticatedStudentId);
            studentIdInput.setDisable(true);
            passwordInput.clear();
            passwordInput.setDisable(true);
            loginButton.setDisable(true);
            return;
        }
        showFailure(result == null || result.message() == null || result.message().isBlank()
                ? "Sai MSSV/mật khẩu hoặc tài khoản đã bị khóa."
                : result.message());
    }

    private void showFailure(String message) {
        feedback.setText(message);
        feedback.getStyleClass().removeAll("muted", "login-success");
        if (!feedback.getStyleClass().contains("login-error")) {
            feedback.getStyleClass().add("login-error");
        }
        if (loginButton != null) {
            loginButton.setDisable(false);
        }
        if (passwordInput != null) {
            passwordInput.clear();
        }
    }

    private void select(Button selected) {
        loginNav.getStyleClass().remove("nav-active");
        homeNav.getStyleClass().remove("nav-active");
        selected.getStyleClass().add("nav-active");
    }

    /** Updates the network status shown in the Client shell. */
    public void setNetworkStatus(String status) {
        if (status == null || status.isBlank()) {
            return;
        }
        Platform.runLater(() -> clientStatus.setText("Client · " + status));
    }

    /** Supplies the real network login operation after the shell is loaded. */
    public void setLoginAction(LoginAction loginAction) {
        this.loginAction = loginAction;
    }

    /** Replaces the current Client screen without creating another stage. */
    public void showContent(Node content, String title) {
        if (content == null || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Content and title are required");
        }
        pageTitle.setText(title);
        contentOutlet.getChildren().setAll(content);
    }

    /** Loads a future Client feature FXML into the shell. */
    public void showFxml(String resourcePath, String title) throws IOException {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IOException("FXML resource not found: " + resourcePath);
        }
        showContent(FXMLLoader.load(url), title);
    }

    @FunctionalInterface
    public interface LoginAction {
        LoginAckMessage login(String studentId, char[] password) throws IOException;
    }
}
