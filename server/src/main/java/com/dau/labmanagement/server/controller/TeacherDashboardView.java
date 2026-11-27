package com.dau.labmanagement.server.controller;

import com.dau.labmanagement.server.model.service.dashboard.MachineSessionRegistry;
import com.dau.labmanagement.server.model.service.dashboard.MachineSnapshot;
import com.dau.labmanagement.server.model.service.dashboard.MachineStatus;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class TeacherDashboardView extends VBox {

    private final MachineSessionRegistry registry;

    private final Label totalValue = new Label("0");
    private final Label onlineValue = new Label("0");
    private final Label unstableValue = new Label("0");
    private final Label offlineValue = new Label("0");

    private final Label updatedLabel = new Label();
    private final Label emptyLabel = new Label();

    private final TextField search = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final FlowPane machineGrid = new FlowPane(14, 14);

    private final Timeline refreshTimer;

    private List<MachineSnapshot> previousSnapshots = List.of();

    public TeacherDashboardView(MachineSessionRegistry registry) {
        this.registry = Objects.requireNonNull(registry);

        setSpacing(10);
        setFillWidth(true);

        Label subtitle = new Label(
                "Theo dõi trạng thái kết nối và chất lượng mạng của phòng máy");
        subtitle.getStyleClass().add("muted");
        subtitle.setWrapText(true);

        updatedLabel.getStyleClass().add("muted");

        HBox statistics = new HBox(12);

        statistics.getChildren().addAll(
                statistic("Tổng số máy", totalValue, "#1E40AF"),
                statistic("Ổn định", onlineValue, "#16A34A"),
                statistic("Không ổn định", unstableValue, "#D97706"),
                statistic("Ngoại tuyến", offlineValue, "#DC2626")
        );

        search.setPromptText("Tìm tên máy, IP, sinh viên...");
        search.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(search, Priority.ALWAYS);

        statusFilter.getItems().setAll(
                "Tất cả trạng thái",
                "Ổn định",
                "Không ổn định",
                "Ngoại tuyến"
        );

        statusFilter.getSelectionModel().selectFirst();

        HBox filters = new HBox(12, search, statusFilter);
        filters.setAlignment(Pos.CENTER_LEFT);

        machineGrid.setAlignment(Pos.TOP_LEFT);
        machineGrid.setPadding(new Insets(2));

        emptyLabel.getStyleClass().add("muted");
        emptyLabel.setWrapText(true);
        emptyLabel.setPadding(new Insets(24));

        VBox gridContent = new VBox(12, emptyLabel, machineGrid);

        ScrollPane scroll = new ScrollPane(gridContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;"
        );

        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label legend = new Label(
                "Xanh: ổn định  |  Vàng: cảnh báo  |  Đỏ: ngoại tuyến");
        legend.getStyleClass().add("muted");
        legend.setWrapText(true);

        getChildren().addAll(
                subtitle,
                statistics,
                filters,
                legend,
                scroll,
                updatedLabel
        );

        search.textProperty().addListener(
                (observable, oldValue, newValue) -> refresh(true));

        statusFilter.valueProperty().addListener(
                (observable, oldValue, newValue) -> refresh(true));

        refreshTimer = new Timeline(
                new KeyFrame(Duration.seconds(1),
                        event -> refresh(false))
        );

        refreshTimer.setCycleCount(Timeline.INDEFINITE);

        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                refreshTimer.stop();
            } else {
                refresh(true);
                refreshTimer.play();
            }
        });

        refresh(true);
    }

    private VBox statistic(String title, Label value, String color) {
        Label heading = new Label(title);
        heading.getStyleClass().add("muted");
        heading.setWrapText(true);

        value.setStyle(
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + color + ";"
        );

        VBox card = new VBox(8, heading, value);
        card.setPadding(new Insets(10));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(0);

        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 10;"
        );

        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void refresh(boolean force) {
        List<MachineSnapshot> snapshots = registry.snapshots();

        totalValue.setText(String.valueOf(snapshots.size()));

        onlineValue.setText(String.valueOf(
                count(snapshots, MachineStatus.ONLINE)));

        unstableValue.setText(String.valueOf(
                count(snapshots, MachineStatus.UNSTABLE)));

        offlineValue.setText(String.valueOf(
                count(snapshots, MachineStatus.OFFLINE)));

        updatedLabel.setText(
                "Cập nhật lúc: " + LocalTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss"))
        );

        if (!force && snapshots.equals(previousSnapshots)) {
            return;
        }

        previousSnapshots = List.copyOf(snapshots);

        String keyword = search.getText()
                .trim()
                .toLowerCase(Locale.ROOT);

        machineGrid.getChildren().clear();

        for (MachineSnapshot machine : snapshots) {
            if (matchesSearch(machine, keyword)
                    && matchesStatus(machine)) {
                machineGrid.getChildren().add(machineCard(machine));
            }
        }

        boolean empty = machineGrid.getChildren().isEmpty();

        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);

        emptyLabel.setText(snapshots.isEmpty()
                ? "Chưa có máy Client kết nối đến Server."
                : "Không tìm thấy máy phù hợp với bộ lọc.");
    }

    private long count(List<MachineSnapshot> machines,
                       MachineStatus status) {
        return machines.stream()
                .filter(machine -> machine.status() == status)
                .count();
    }

    private boolean matchesSearch(MachineSnapshot machine,
                                  String keyword) {

        String text = safe(machine.hostname()) + " "
                + safe(machine.ipAddress()) + " "
                + safe(machine.seatLabel()) + " "
                + safe(machine.studentId()) + " "
                + safe(machine.studentName());

        return text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesStatus(MachineSnapshot machine) {
        String selected = statusFilter.getValue();

        if (selected == null || selected.equals("Tất cả trạng thái")) {
            return true;
        }

        return switch (selected) {
            case "Ổn định" -> machine.status() == MachineStatus.ONLINE;
            case "Không ổn định" ->
                    machine.status() == MachineStatus.UNSTABLE;
            case "Ngoại tuyến" ->
                    machine.status() == MachineStatus.OFFLINE;
            default -> true;
        };
    }

    private VBox machineCard(MachineSnapshot machine) {

        String color = machine.status().getColor();

        Circle dot = new Circle(6, Color.web(color));

        Label hostname = new Label(
                fallback(machine.hostname(), "Máy chưa đặt tên"));

        hostname.setStyle(
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1F2937;"
        );

        hostname.setWrapText(true);
        HBox.setHgrow(hostname, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, hostname, spacer, dot);
        header.setAlignment(Pos.CENTER_LEFT);

        Label status = new Label(machine.status().getLabel());

        status.setStyle(
                "-fx-text-fill: " + color + ";" +
                "-fx-font-weight: bold;"
        );

        Label seat = new Label(
                "Ghế máy: " + fallback(machine.seatLabel(), "Chưa gán"));

        Label ip = new Label(
                "IP: " + fallback(machine.ipAddress(), "—"));

        Label student = new Label(fallback(machine.displayStudent(), "Chưa đăng nhập"));
        student.setWrapText(true);
        student.setMinHeight(Region.USE_PREF_SIZE);
        student.setStyle("-fx-font-weight: bold; -fx-text-fill: #1F2937;");

        Label studentId = new Label(
                "MSSV: " + fallback(machine.studentId(), "—"));

        seat.getStyleClass().add("muted");
        ip.getStyleClass().add("muted");
        studentId.getStyleClass().add("muted");

        Region separator = new Region();
        separator.setMinHeight(1);
        separator.setStyle("-fx-background-color: #E2E8F0;");

        String rtt = machine.status() == MachineStatus.OFFLINE
                ? "—" : machine.displayRtt();

        String loss = machine.status() == MachineStatus.OFFLINE
                ? "—" : machine.displayPacketLoss();

        HBox metrics = new HBox(
                18,
                metric("RTT", rtt),
                metric("Packet loss", loss)
        );

        VBox card = new VBox(
                4,
                header,
                status,
                seat,
                ip,
                separator,
                student,
                studentId,
                metrics
        );

        card.setPrefWidth(270);
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(10));

        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1 1 1 4;" +
                "-fx-border-color: #E2E8F0 #E2E8F0 #E2E8F0 "
                        + color + ";"
        );

        return card;
    }

    private VBox metric(String title, String value) {
        Label heading = new Label(title);
        heading.getStyleClass().add("muted");

        Label result = new Label(fallback(value, "—"));
        result.setMinHeight(Region.USE_PREF_SIZE);
        result.setTextFill(Color.web("#1F2937"));
        result.setStyle(
                "-fx-font-size: 16px; -fx-text-fill: #1F2937;" +
                "-fx-font-weight: bold;"
        );

        VBox metricBox = new VBox(4, heading, result);
        metricBox.setMinHeight(Region.USE_PREF_SIZE);
        return metricBox;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank()
                ? defaultValue : value;
    }
}