package com.darkmusic.stickyimagemgr;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ManagerApplication extends Application {

    @Override
    public void start(Stage stage) {
        var controller = new ManagerController();
        controller.loadAppPrefs(getDefaultAppSettingsPath());

        var scene = new Scene(controller.createContent());
        stage.setTitle("Sticky Image Manager");
        stage.setScene(scene);
        stage.show();
        controller.setStage(stage);
        controller.logText("Ready.");
    }

    public static void main(String[] args) {
        launch();
    }

    String getDefaultAppSettingsPath() {
        return System.getProperty("user.home") + "/.stickyimagemgr";
    }
}