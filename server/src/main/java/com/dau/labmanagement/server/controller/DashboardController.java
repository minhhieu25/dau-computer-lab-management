package com.dau.labmanagement.server.controller;

import com.dau.labmanagement.server.model.entity.UserRole;
import com.dau.labmanagement.server.model.service.LoginSession;
import com.dau.labmanagement.server.model.service.dashboard.MachineSessionRegistry;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * CONTROLLER for the post-login shell (SC_003 for teachers, SC_004 for admins).
 *
 * <p>Renders only the navigation entries allowed for the authenticated role
 * (BR-09: an admin never controls the class, a teacher never manages accounts).
 * Content areas are placeholders until the corresponding screen tasks land.</p>
 */
public final class DashboardController {

    @FXML private Label identityLabel;
    @FXML private Label roleLabel;
    @FXML private Label navigationTitle;
    @FXML private Label pageTitle;
    @FXML private Label statusLabel;
    @FXML private VBox navigation;
    @FXML private StackPane contentOutlet;
    @FXML private Button logoutButton;

    private LoginSession session;
    private MachineSessionRegistry machineRegistry;
    private Runnable onLogout = () -> { };

    /** Applies the authenticated session and builds the role-scoped navigation. */
    public void setSession(LoginSession session) {
        this.session = Objects.requireNonNull(session, "session");
        identityLabel.setText(session.fullName() + " · " + session.roleLabel());
        roleLabel.setText("Vai trò: " + session.roleLabel() + " (" + session.username() + ")");
        statusLabel.setText("Server · Đang đăng nhập: " + session.username());
        navigationTitle.setText(session.role() == UserRole.ADMIN
                ? "QUẢN TRỊ HỆ THỐNG"
                : "ĐIỀU KHIỂN LỚP HỌC");
        renderNavigation();
    }

    /** Sets the callback invoked by the logout button. */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
    }

    /** Supplies the live machine state used by the teacher dashboard. */
    public void setMachineRegistry(MachineSessionRegistry machineRegistry) {
        this.machineRegistry = machineRegistry;
    }

    /** Returns the active session, or null before {@link #setSession(LoginSession)}. */
    public LoginSession getSession() {
        return session;
    }

    @FXML
    private void onLogout() {
        if (session != null) {
            statusLabel.setText("Server · Đã đăng xuất");
        }
        onLogout.run();
    }

    private void renderNavigation() {
        navigation.getChildren().clear();
        Map<String, String> pages = session.role() == UserRole.ADMIN ? adminPages() : teacherPages();
        pages.forEach((title, description) -> {
            Button button = new Button(title);
            button.getStyleClass().add("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> {
                navigation.getChildren().forEach(node -> node.getStyleClass().remove("nav-active"));
                button.getStyleClass().add("nav-active");
                if (session.role() == UserRole.TEACHER
                        && "Dashboard phòng máy".equals(title)) {
                    showTeacherDashboard(title, description);
                } else {
                    showPlaceholder(title, description);
                }
            });
            navigation.getChildren().add(button);
        });
        if (!navigation.getChildren().isEmpty()) {
            ((Button) navigation.getChildren().get(0)).fire();
        }
    }

    private Map<String, String> teacherPages() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("Dashboard phòng máy", "Lưới máy kèm trạng thái kết nối, RTT và mất gói.");
        pages.put("Giám sát màn hình", "Xem màn hình của máy sinh viên được chọn.");
        pages.put("Khóa / Mở khóa", "Khóa hoặc mở khóa máy sinh viên.");
        pages.put("Trình chiếu bài giảng", "Phát bài giảng tới các máy sinh viên.");
        pages.put("Gửi tài liệu", "Gửi tài liệu vào thư mục DeBai của sinh viên.");
        pages.put("Bản ghi của tôi", "Các bản ghi do chính giảng viên này tạo.");
        return pages;
    }

    private Map<String, String> adminPages() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("Tổng quan quản trị", "Tổng quan hệ thống và các lối tắt quản trị.");
        pages.put("Quản lý tài khoản", "Tạo, khóa/mở khóa, đặt lại mật khẩu và xóa tài khoản.");
        pages.put("Quản lý bản ghi", "Xem, xóa bản ghi và cấu hình thời hạn lưu.");
        return pages;
    }

    private void showPlaceholder(String title, String description) {
        pageTitle.setText(title);
        VBox panel = new VBox(14);
        panel.getStyleClass().add("surface");
        panel.setMaxWidth(Double.MAX_VALUE);
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        Label info = new Label(description);
        info.getStyleClass().add("body-text");
        info.setWrapText(true);
        Label note = new Label("Chức năng đang được hoàn thiện.");
        note.getStyleClass().add("muted");
        note.setWrapText(true);
        panel.getChildren().addAll(heading, info, note);
        contentOutlet.getChildren().setAll(panel);
    }

    private void showTeacherDashboard(String title, String description) {
        pageTitle.setText(title);
        if (machineRegistry != null) {
            contentOutlet.getChildren().setAll(new TeacherDashboardView(machineRegistry));
            return;
        }

        VBox panel = new VBox(14);
        panel.getStyleClass().add("surface");
        panel.setMaxWidth(Double.MAX_VALUE);
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        Label info = new Label(description);
        info.getStyleClass().add("body-text");
        info.setWrapText(true);
        Label note = new Label("Không thể tải trạng thái máy vì Server network runtime chưa khởi động.");
        note.getStyleClass().add("muted");
        note.setWrapText(true);
        panel.getChildren().addAll(heading, info, note);
        contentOutlet.getChildren().setAll(panel);
    }
}
