package com.dau.labmanagement.server.view.filepush;

import com.dau.labmanagement.server.model.state.TransferProgress;
import com.dau.labmanagement.server.model.state.TransferStatus;
import com.dau.labmanagement.server.service.filepush.FileDistributionAuthorizationException;
import com.dau.labmanagement.server.service.filepush.FileDistributionService;
import com.dau.labmanagement.server.service.filepush.FileTransferAttempt;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Standalone teacher UI for selecting files/recipients and observing SC_010 transfer attempts. */
public final class FileDistributionPane extends BorderPane implements AutoCloseable {
    private final FileDistributionService distributionService;
    private final Supplier<String> teacherSessionIdProvider;
    private final Supplier<List<RecipientOption>> recipientProvider;
    private final Executor backgroundExecutor;
    private final Window owner;
    private final ObservableList<Path> selectedFiles = FXCollections.observableArrayList();
    private final ObservableList<RecipientOption> availableRecipients = FXCollections.observableArrayList();
    private final ObservableList<TransferRow> transferRows = FXCollections.observableArrayList();
    private final ListView<Path> fileList = new ListView<>(selectedFiles);
    private final ListView<RecipientOption> recipientList = new ListView<>(availableRecipients);
    private final TableView<TransferRow> transferTable = new TableView<>(transferRows);
    private final Button chooseFilesButton = new Button("Chọn file");
    private final Button refreshRecipientsButton = new Button("Làm mới máy");
    private final Button sendButton = new Button("Gửi đến máy đã chọn");
    private final Button cancelButton = new Button("Hủy transfer đã chọn");
    private final Label statusLabel = new Label("Chọn file và máy nhận.");
    private final Timeline progressRefresh = new Timeline(new KeyFrame(
            javafx.util.Duration.millis(350), event -> transferTable.refresh()));

    public FileDistributionPane(
            Window owner,
            FileDistributionService distributionService,
            Supplier<String> teacherSessionIdProvider,
            Supplier<List<RecipientOption>> recipientProvider,
            Executor backgroundExecutor) {
        this.owner = owner;
        this.distributionService = Objects.requireNonNull(distributionService, "distributionService");
        this.teacherSessionIdProvider = Objects.requireNonNull(teacherSessionIdProvider, "teacherSessionIdProvider");
        this.recipientProvider = Objects.requireNonNull(recipientProvider, "recipientProvider");
        this.backgroundExecutor = Objects.requireNonNull(backgroundExecutor, "backgroundExecutor");

        buildLayout();
        refreshRecipients();
        progressRefresh.setCycleCount(Timeline.INDEFINITE);
        progressRefresh.play();
    }

    private void buildLayout() {
        setPadding(new javafx.geometry.Insets(18));
        Label title = new Label("Phát tài liệu");
        title.getStyleClass().add("section-title");

        fileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        recipientList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        recipientList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(RecipientOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else {
                    setText(item.displayName() + " · " + item.machineId() + " · " + item.studentId());
                    setDisable(!item.selectable());
                }
            }
        });

        TableColumn<TransferRow, String> fileColumn = column("File", row -> row.fileName());
        TableColumn<TransferRow, String> machineColumn = column("Máy", row -> row.progress().machineId());
        TableColumn<TransferRow, String> studentColumn = column(
                "MSSV", row -> row.progress().studentId().orElse("Chưa đăng nhập"));
        TableColumn<TransferRow, String> stateColumn = column(
                "Trạng thái", row -> row.progress().status().name());
        TableColumn<TransferRow, String> progressColumn = column(
                "%", row -> String.format("%.1f%%", row.progress().percentComplete()));
        TableColumn<TransferRow, String> speedColumn = column(
                "Mbps", row -> String.format("%.2f", row.progress().speedMbps()));
        TableColumn<TransferRow, String> etaColumn = column("ETA", row -> formatEta(row.progress().eta()));
        transferTable.getColumns().setAll(List.of(
            fileColumn, machineColumn, studentColumn, stateColumn, progressColumn, speedColumn, etaColumn));

        chooseFilesButton.setOnAction(event -> chooseFiles());
        refreshRecipientsButton.setOnAction(event -> refreshRecipients());
        sendButton.getStyleClass().add("primary-button");
        sendButton.setOnAction(event -> startDistribution());
        cancelButton.setOnAction(event -> cancelSelectedTransfer());

        HBox actions = new HBox(10, chooseFilesButton, refreshRecipientsButton, sendButton, cancelButton);
        VBox top = new VBox(12, title, actions, statusLabel);
        setTop(top);

        VBox files = new VBox(8, new Label("File nguồn"), fileList);
        VBox recipients = new VBox(8, new Label("Máy Online có phiên sinh viên"), recipientList);
        VBox.setVgrow(fileList, Priority.ALWAYS);
        VBox.setVgrow(recipientList, Priority.ALWAYS);
        SplitPane selectors = new SplitPane(files, recipients);
        selectors.setDividerPositions(0.5);
        VBox center = new VBox(12, selectors, new Label("Tiến độ theo từng máy"), transferTable);
        VBox.setVgrow(selectors, Priority.ALWAYS);
        VBox.setVgrow(transferTable, Priority.ALWAYS);
        setCenter(center);
    }

    private void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn tài liệu phát");
        List<java.io.File> files = chooser.showOpenMultipleDialog(owner);
        if (files == null || files.isEmpty()) {
            return;
        }
        selectedFiles.setAll(files.stream().map(java.io.File::toPath).toList());
        statusLabel.setText(selectedFiles.size() + " file đã chọn.");
    }

    private void refreshRecipients() {
        try {
            List<RecipientOption> options = recipientProvider.get();
            availableRecipients.setAll(options.stream().filter(RecipientOption::selectable).toList());
            statusLabel.setText(availableRecipients.size() + " máy Online có sinh viên đăng nhập.");
        } catch (RuntimeException exception) {
            availableRecipients.clear();
            statusLabel.setText("Không thể tải danh sách máy nhận.");
        }
    }

    private void startDistribution() {
        List<Path> files = List.copyOf(selectedFiles);
        List<String> machineIds = recipientList.getSelectionModel().getSelectedItems().stream()
                .filter(RecipientOption::selectable)
                .map(RecipientOption::machineId)
                .distinct()
                .toList();
        if (files.isEmpty() || machineIds.isEmpty()) {
            statusLabel.setText("Chọn ít nhất một file và một máy nhận hợp lệ.");
            return;
        }

        String teacherSessionId;
        try {
            teacherSessionId = teacherSessionIdProvider.get();
        } catch (RuntimeException exception) {
            statusLabel.setText("Không thể xác minh phiên giảng viên hiện tại.");
            return;
        }
        sendButton.setDisable(true);
        statusLabel.setText("Đang khởi tạo các transfer...");
        try {
            backgroundExecutor.execute(() -> distributeFiles(files, machineIds, teacherSessionId));
        } catch (RuntimeException exception) {
            sendButton.setDisable(false);
            statusLabel.setText("Không thể bắt đầu transfer trên worker executor.");
        }
    }

    private void distributeFiles(List<Path> files, List<String> machineIds, String teacherSessionId) {
        List<TransferRow> started = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (Path file : files) {
            try {
                distributionService.distribute(file, machineIds, teacherSessionId).stream()
                        .map(attempt -> new TransferRow(file.getFileName().toString(), attempt))
                        .forEach(started::add);
            } catch (IOException | FileDistributionAuthorizationException | RuntimeException exception) {
                failures.add(file.getFileName() + ": " + safeMessage(exception));
            }
        }

        Platform.runLater(() -> {
            transferRows.addAll(started);
            sendButton.setDisable(false);
            if (failures.isEmpty()) {
                statusLabel.setText(started.size() + " transfer đã khởi tạo.");
            } else {
                statusLabel.setText("Một số file không khởi tạo được: " + String.join("; ", failures));
            }
        });
    }

    private void cancelSelectedTransfer() {
        TransferRow selected = transferTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Chọn một transfer để hủy.");
            return;
        }
        try {
            backgroundExecutor.execute(() -> {
                boolean cancelled = selected.attempt().cancel();
                Platform.runLater(() -> statusLabel.setText(cancelled
                        ? "Đã gửi yêu cầu hủy transfer."
                        : "Transfer đã kết thúc hoặc không thể hủy."));
            });
        } catch (RuntimeException exception) {
            statusLabel.setText("Không thể gửi yêu cầu hủy.");
        }
    }

    private static TableColumn<TransferRow, String> column(String title,
                                                           java.util.function.Function<TransferRow, String> value) {
        TableColumn<TransferRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private static String formatEta(java.util.Optional<Duration> eta) {
        return eta.map(value -> value.getSeconds() + " s").orElse("Đang tính");
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    @Override
    public void close() {
        progressRefresh.stop();
        for (TransferRow row : transferRows) {
            if (row.progress().status() == TransferStatus.PENDING
                    || row.progress().status() == TransferStatus.TRANSFERRING) {
                try {
                    backgroundExecutor.execute(row.attempt()::cancel);
                } catch (RuntimeException ignored) {
                    statusLabel.setText("Một số transfer không thể được hủy khi đóng màn hình.");
                }
            }
        }
    }

    public record RecipientOption(
            String machineId,
            String studentId,
            String displayName,
            boolean online,
            boolean studentAuthenticated,
            boolean sessionValid) {
        public RecipientOption {
            Objects.requireNonNull(machineId, "machineId");
            Objects.requireNonNull(studentId, "studentId");
            Objects.requireNonNull(displayName, "displayName");
        }

        public boolean selectable() {
            return online && studentAuthenticated && sessionValid;
        }
    }

    private record TransferRow(String fileName, FileTransferAttempt attempt) {
        private TransferProgress progress() {
            return attempt.progress();
        }
    }
}