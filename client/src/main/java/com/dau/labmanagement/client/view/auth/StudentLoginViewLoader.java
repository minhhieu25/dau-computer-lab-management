package com.dau.labmanagement.client.view.auth;

import com.dau.labmanagement.client.controller.auth.StudentLoginController;
import com.dau.labmanagement.client.viewmodel.StudentLoginViewModel;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Executor;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/** Loads the SC_002 view independently so FND-04 can attach it to the Client shell. */
public final class StudentLoginViewLoader {
    private StudentLoginViewLoader() {
    }

    /** Loads and configures the login view without changing the shell or creating another stage. */
    public static Parent load(StudentLoginViewModel viewModel, Executor backgroundExecutor) throws IOException {
        Objects.requireNonNull(viewModel, "viewModel");
        Objects.requireNonNull(backgroundExecutor, "backgroundExecutor");
        URL resource = StudentLoginViewLoader.class.getResource("/fxml/student-login.fxml");
        if (resource == null) {
            throw new IOException("SC_002 FXML resource not found");
        }

        FXMLLoader loader = new FXMLLoader(resource);
        Parent view = loader.load();
        loader.<StudentLoginController>getController().configure(viewModel, backgroundExecutor);
        return view;
    }
}