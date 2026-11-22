package com.dau.labmanagement.client.controller.auth;

import com.dau.labmanagement.client.model.entity.StudentIdentity;
import com.dau.labmanagement.client.service.auth.StudentLoginService;
import com.dau.labmanagement.client.viewmodel.StudentLoginViewModel;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/** Controller for the SC_002 login view; dependencies are supplied by the owning application. */
public final class StudentLoginController {
    @FXML private TextField studentIdField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button logoutButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label feedbackLabel;
    @FXML private Label identityLabel;

    private StudentLoginViewModel viewModel;
    private Executor backgroundExecutor;
    private boolean busy;

    @FXML
    private void initialize() {
        setBusy(false);
        logoutButton.setVisible(false);
        logoutButton.setManaged(false);
        identityLabel.setText("Chưa có sinh viên đăng nhập.");
    }

    /** Configures the view after FXML loading without depending on the FND-04 shell. */
    public void configure(StudentLoginViewModel viewModel, Executor backgroundExecutor) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.backgroundExecutor = Objects.requireNonNull(backgroundExecutor, "backgroundExecutor");
        feedbackLabel.setText(viewModel.feedback());
        updateIdentity(viewModel.studentIdentity().orElse(null));
    }

    @FXML
    private void handleLogin() {
        if (busy) {
            return;
        }
        if (!isConfigured()) {
            feedbackLabel.setText("Màn hình đăng nhập chưa được kết nối với dịch vụ xác thực.");
            return;
        }

        String studentId = studentIdField.getText();
        char[] password = passwordField.getText().toCharArray();
        passwordField.clear();

        if (studentId == null || studentId.isBlank()) {
            Arrays.fill(password, '\0');
            feedbackLabel.setText("Vui lòng nhập MSSV.");
            studentIdField.requestFocus();
            return;
        }
        if (password.length == 0) {
            feedbackLabel.setText("Vui lòng nhập mật khẩu.");
            passwordField.requestFocus();
            return;
        }

        setBusy(true);
        try {
            backgroundExecutor.execute(() -> {
                StudentLoginService.LoginStatus status = viewModel.submitLogin(studentId, password);
                String feedback = viewModel.feedback();
                StudentIdentity identity = viewModel.studentIdentity().orElse(null);
                Platform.runLater(() -> {
                    setBusy(false);
                    feedbackLabel.setText(feedback);
                    updateIdentity(identity);
                    if (status == StudentLoginService.LoginStatus.AUTHENTICATED) {
                        studentIdField.clear();
                    }
                });
            });
        } catch (RejectedExecutionException exception) {
            Arrays.fill(password, '\0');
            setBusy(false);
            feedbackLabel.setText("Không thể bắt đầu yêu cầu đăng nhập.");
        }
    }

    @FXML
    private void handleLogout() {
        if (busy || !isConfigured() || viewModel.studentIdentity().isEmpty()) {
            return;
        }

        setBusy(true);
        try {
            backgroundExecutor.execute(() -> {
                viewModel.logout();
                String feedback = viewModel.feedback();
                Platform.runLater(() -> {
                    setBusy(false);
                    feedbackLabel.setText(feedback);
                    updateIdentity(null);
                });
            });
        } catch (RejectedExecutionException exception) {
            setBusy(false);
            feedbackLabel.setText("Không thể bắt đầu yêu cầu đăng xuất.");
        }
    }

    private boolean isConfigured() {
        return viewModel != null && backgroundExecutor != null;
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        if (loginButton == null) {
            return;
        }
        loginButton.setDisable(busy);
        logoutButton.setDisable(busy);
        studentIdField.setDisable(busy);
        passwordField.setDisable(busy);
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);
    }

    private void updateIdentity(StudentIdentity identity) {
        if (identity == null) {
            identityLabel.setText("Chưa có sinh viên đăng nhập.");
            logoutButton.setVisible(false);
            logoutButton.setManaged(false);
            return;
        }
        identityLabel.setText(identity.displayName() + " · " + identity.studentId());
        logoutButton.setVisible(true);
        logoutButton.setManaged(true);
    }
}