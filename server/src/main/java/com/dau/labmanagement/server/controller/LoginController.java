package com.dau.labmanagement.server.controller;

import com.dau.labmanagement.server.model.service.LoginFailureReason;
import com.dau.labmanagement.server.model.service.LoginResult;
import com.dau.labmanagement.server.model.service.LoginService;
import com.dau.labmanagement.server.model.service.LoginSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * CONTROLLER for SC_001 — Server login.
 *
 * <p>Delegates every check to {@link LoginService}; this class only reads the
 * form, shows the returned error message and hands a successful
 * {@link LoginSession} to the navigation callback. It never touches DAOs.</p>
 */
public final class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;

    private LoginService loginService = LoginService.unavailable();
    private Consumer<LoginSession> onLoginSuccess = session -> { };

    /** Injects the service used to authenticate; called by ServerApp before showing the view. */
    public void setLoginService(LoginService loginService) {
        this.loginService = Objects.requireNonNull(loginService, "loginService");
    }

    /** Sets the callback that navigates to the dashboard of the authenticated role. */
    public void setOnLoginSuccess(Consumer<LoginSession> onLoginSuccess) {
        this.onLoginSuccess = Objects.requireNonNull(onLoginSuccess, "onLoginSuccess");
    }

    @FXML
    private void initialize() {
        hideError();
        loginButton.setDefaultButton(true);
        usernameField.requestFocus();
    }

    @FXML
    private void onLogin() {
        hideError();
        String username = usernameField.getText();
        char[] password = passwordField.getText().toCharArray();
        if (password.length == 0) {
            showError(LoginFailureReason.EMPTY_INPUT.message());
            passwordField.requestFocus();
            return;
        }

        LoginResult result;
        try {
            result = loginService.login(username, password);
        } catch (RuntimeException exception) {
            LOG.error("Login call failed unexpectedly", exception);
            result = LoginResult.rejected(LoginFailureReason.SYSTEM_UNAVAILABLE);
        } finally {
            Arrays.fill(password, '\0');
        }

        if (!result.isSuccess()) {
            passwordField.clear();
            passwordField.requestFocus();
            showError(result.failureMessage());
            return;
        }

        LoginSession session = result.session();
        passwordField.clear();
        statusLabel.setText("Đã đăng nhập: " + session.fullName() + " · " + session.roleLabel());
        onLoginSuccess.accept(session);
    }

    private void showError(String message) {
        errorLabel.setText(message == null ? "" : message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
