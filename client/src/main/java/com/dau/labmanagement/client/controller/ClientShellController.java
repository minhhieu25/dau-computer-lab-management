package com.dau.labmanagement.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import javafx.application.Platform;

/** Client shell navigation; real authentication and networking are out of scope. */
public final class ClientShellController {
    @FXML private StackPane contentOutlet;
    @FXML private Label pageTitle;
    @FXML private Label clientStatus;
    @FXML private Button loginNav;
    @FXML private Button homeNav;
    private String demoStudentId = "";

    @FXML
    private void initialize() {
        showLogin();
    }

    @FXML
    private void showLogin() {
        select(loginNav);
        pageTitle.setText("Đăng nhập sinh viên");
        VBox form = new VBox(14);
        form.getStyleClass().add("surface");
        form.setMaxWidth(560);
        Label heading = new Label("Màn hình đăng nhập");
        heading.getStyleClass().add("section-title");
        Label hint = new Label("Giao diện minh họa · chưa xác thực với Server");
        hint.getStyleClass().add("muted");
        TextField studentId = new TextField(demoStudentId);
        studentId.setPromptText("Mã sinh viên");
        Label feedback = new Label("Chưa đăng nhập");
        feedback.getStyleClass().add("muted");
        Button demoButton = new Button("Vào màn hình chính (minh họa)");
        demoButton.getStyleClass().add("primary-button");
        demoButton.setOnAction(event -> {
            String value = studentId.getText().trim();
            if (value.isEmpty()) {
                feedback.setText("Vui lòng nhập mã sinh viên.");
                return;
            }
            demoStudentId = value;
            showHome();
        });
        form.getChildren().addAll(heading, hint, studentId, demoButton, feedback);
        contentOutlet.getChildren().setAll(form);
        clientStatus.setText("Client · Chưa kết nối Server (minh họa)");
    }

    @FXML
    private void showHome() {
        select(homeNav);
        pageTitle.setText("Trang chính sinh viên");
        VBox panel = new VBox(14);
        panel.getStyleClass().add("surface");
        Label heading = new Label("Khu vực chức năng Client");
        heading.getStyleClass().add("section-title");
        Label identity = new Label(demoStudentId.isEmpty()
                ? "Chưa đăng nhập · chỉ xem thử giao diện"
                : "Sinh viên (minh họa): " + demoStudentId);
        Label description = new Label("Vùng tích hợp màn hình Client sau này: thông báo, nhận tài liệu và trạng thái máy.");
        description.setWrapText(true);
        description.getStyleClass().add("body-text");
        Label note = new Label("Chưa có xác thực, TCP/UDP hoặc giám sát thực tế.");
        note.getStyleClass().add("muted");
        panel.getChildren().addAll(heading, identity, description, note);
        contentOutlet.getChildren().setAll(panel);
        clientStatus.setText("Client · Chưa kết nối Server (minh họa)");
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
}