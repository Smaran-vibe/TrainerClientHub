package com.trainerclienthub.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utility class for loading FXML views and switching scenes.
 */
public class ViewLoader {

    private static final String FXML_BASE = "/com/trainerclienthub/";

    public static void navigateTo(Stage stage, String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ViewLoader.class.getResource(FXML_BASE + fxmlFile));
            Parent root = loader.load();

            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            stage.setTitle(title);
            stage.show();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxmlFile, e);
        }
    }

    public static FXMLLoader getLoader(String fxmlFile) {
        return new FXMLLoader(
                ViewLoader.class.getResource(FXML_BASE + fxmlFile));
    }
}
