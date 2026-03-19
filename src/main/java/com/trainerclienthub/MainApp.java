package com.trainerclienthub;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.util.ViewLoader;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

// Entry point for the Trainer Client Hub JavaFX desktop application.

public class MainApp extends Application {

    private static final String APP_TITLE  = "Trainer Client Hub";
    private static final double MIN_WIDTH  = 1080;
    private static final double MIN_HEIGHT = 800;
    private static final double INIT_WIDTH = 1200;
    private static final double INIT_HEIGHT = 850;

    // JavaFX lifecycle

    @Override
    public void start(Stage primaryStage) {
        configurePrimaryStage(primaryStage);
        ViewLoader.navigateTo(primaryStage, "LoginView.fxml", APP_TITLE + " — Login");
        primaryStage.show();
    }

    /** Close the shared JDBC connection when the app exits. */
    @Override
    public void stop() {
        DatabaseConnection.getInstance().close();
    }


    public static void main(String[] args) {
        launch(args);
    }

    //  Private helpers

    private void configurePrimaryStage(Stage stage) {
        stage.setTitle(APP_TITLE);
        stage.setWidth(INIT_WIDTH);
        stage.setHeight(INIT_HEIGHT);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.centerOnScreen();

        stage.setOnCloseRequest(event -> {
            DatabaseConnection.getInstance().close();
            Platform.exit();
        });
    }
}
