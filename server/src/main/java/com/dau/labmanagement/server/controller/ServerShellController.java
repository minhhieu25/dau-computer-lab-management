package com.dau.labmanagement.server.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/** Provides role-scoped navigation and a reusable content outlet for Server screens. */
public final class ServerShellController {
    @FXML private ComboBox<String> roleSelector;
    @FXML private VBox navigation;
    @FXML private StackPane contentOutlet;
    @FXML private Label pageTitle;
    @FXML private Label roleLabel;
    @FXML private Label navigationStatus;

    private final Map<String, String> teacherPages = new LinkedHashMap<>();
    private final Map<String, String> adminPages = new LinkedHashMap<>();
    private String currentPage;

    @FXML
    private void initialize() {
        teacherPages.put("Dashboard phòng máy", "SC-003 · Vùng màn hình tổng quan và trạng thái máy");
        teacherPages.put("Giám sát màn hình", "SC-008 · Vùng màn hình live-view");
        teacherPages.put("Điều khiển máy", "SC-006 / SC-007 · Vùng điều khiển máy và trình chiếu");
        teacherPages.put("Gửi tài liệu", "SC-010 · Vùng phát tài liệu");
        teacherPages.put("Bản ghi của tôi", "SC-012 · Vùng xem bản ghi giảng viên");
        adminPages.put("Tổng quan quản trị", "SC-004 · Vùng dashboard quản trị");
        adminPages.put("Quản lý tài khoản", "SC-005 · Vùng quản lý tài khoản");
        adminPages.put("Quản lý bản ghi", "SC-013 · Vùng quản lý bản ghi");
        roleSelector.getItems().setAll("Giảng viên", "Admin");
        roleSelector.getSelectionModel().select("Giảng viên");
        renderNavigation();
    }

    @FXML
    private void onRoleChanged() {
        renderNavigation();
    }

    private void renderNavigation() {
        navigation.getChildren().clear();
        boolean admin = "Admin".equals(roleSelector.getValue());
        roleLabel.setText("Vai trò minh họa: " + (admin ? "Admin" : "Giảng viên"));
        Map<String, String> pages = admin ? adminPages : teacherPages;
        pages.forEach((title, description) -> {
            Button button = new Button(title);
            button.getStyleClass().add("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> {
                navigation.getChildren().forEach(node -> node.getStyleClass().remove("nav-active"));
                button.getStyleClass().add("nav-active");
                showPlaceholder(title, description);
            });
            navigation.getChildren().add(button);
        });
        if (!navigation.getChildren().isEmpty()) {
            ((Button) navigation.getChildren().get(0)).fire();
        }
    }

    private void showPlaceholder(String title, String description) {
        currentPage = title;
        pageTitle.setText(title);
        navigationStatus.setText("Đang xem: " + title);
        VBox panel = new VBox(14);
        panel.getStyleClass().add("surface");
        panel.setMaxWidth(Double.MAX_VALUE);
        Label heading = new Label("Khu vực nội dung chức năng");
        heading.getStyleClass().add("section-title");
        Label info = new Label(description);
        info.getStyleClass().add("body-text");
        info.setWrapText(true);
        Label note = new Label("FND-04: đây là vùng giao diện chờ tích hợp. Chưa có dữ liệu thật hoặc kết nối mạng.");
        note.getStyleClass().add("muted");
        note.setWrapText(true);
        panel.getChildren().addAll(heading, info, note);
        contentOutlet.getChildren().setAll(panel);
    }

    /** Replaces the active content without opening another window. */
    public void showContent(Node content, String title) {
        if (content == null || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Content and title are required");
        }
        pageTitle.setText(title);
        navigationStatus.setText("Đang xem: " + title);
        contentOutlet.getChildren().setAll(content);
    }

    /** Loads a feature FXML into the content outlet, for later team integration. */
    public void showFxml(String resourcePath, String title) throws IOException {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IOException("FXML resource not found: " + resourcePath);
        }
        showContent(FXMLLoader.load(url), title);
    }

    /** Returns the selected demonstration role; authorization belongs to the login module. */
    public String getSelectedRole() {
        return roleSelector.getValue();
    }

    /** Returns the active placeholder route, or null after external content is shown. */
    public String getCurrentPage() {
        return currentPage;
    }
}