package com.allinweb.ch.component.scene;

import com.allinweb.ch.driver.ARWebDriver;
import com.allinweb.ch.util.ARCallback;
import com.allinweb.ch.util.ARConstants;
import com.allinweb.ch.util.ARLogger;
import java.util.Objects;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ARAlertScene {

    public ARAlertScene(Alert.AlertType alertType, String title, String message, ButtonType... buttons) {
        try {
            Alert alert = new Alert(alertType, message, buttons);
            Image icon =
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream(ARConstants.ICON_APPLICATION)));
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().add(icon);
            alert.setTitle(alertType.name());
            alert.setHeaderText(title);
            alertStage.setAlwaysOnTop(true);
            alert.show();
        } catch (Exception e) {
            ARLogger.getInstance(ARWebDriver.class).severe("ARAlertScene\n" + e);
        }
    }

    public ARAlertScene(
            Alert.AlertType alertType, String title, String message, ARCallback callback, ButtonType... buttons) {
        try {
            Alert alert = new Alert(alertType, message, buttons);
            Image icon =
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream(ARConstants.ICON_APPLICATION)));
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().add(icon);
            alert.setTitle(alertType.name());
            alert.setHeaderText(title);
            alertStage.setAlwaysOnTop(true);
            alert.showAndWait()
                    .filter((response) -> {
                        return response == ButtonType.OK;
                    })
                    .ifPresent((response) -> {
                        callback.execute();
                    });
        } catch (Exception e) {
            ARLogger.getInstance(ARWebDriver.class).severe("ARAlertScene\n" + e);
        }
    }
}
