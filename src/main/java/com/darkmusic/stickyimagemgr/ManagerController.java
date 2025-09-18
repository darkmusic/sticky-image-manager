package com.darkmusic.stickyimagemgr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ManagerController {
    private final List<String> recentFiles = new ArrayList<>();
    private List<ViewerController> viewerControllers = new ArrayList<>();
    private Menu openRecentMenu;
    private TextField windowCountTextField;
    private Label currentFileContent;
    private TextArea log;
    private String filename;
    private Stage stage;
    private ManagerPrefs managerPrefs;
    private AppPreferences appPreferences;
    private String appSettingsPath;
    private String lastUsedDirectory;
    private StageStyle currentStageStyle = StageStyle.UNDECORATED;


    public String getLastUsedDirectory() {
        return lastUsedDirectory;
    }

    public void setLastUsedDirectory(String lastUsedDirectory) {
        this.lastUsedDirectory = lastUsedDirectory;
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setAppPreferences(AppPreferences appPreferences, String appSettingsPath) {
        this.appPreferences = appPreferences;
        this.appSettingsPath = appSettingsPath;
        recentFiles.clear();
        if (appPreferences.getRecentFiles() != null) {
            recentFiles.addAll(appPreferences.getRecentFiles());
            updateOpenRecentMenu();
        }
    }

    void saveAppPreferences() {
        appPreferences.setRecentFiles(recentFiles);
        try {
            var mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File(appSettingsPath), appPreferences);
        } catch (JsonProcessingException e) {
            logText("Error processing JSON: " + e.getMessage());
            logText(Arrays.toString(e.getStackTrace()));
        } catch (IOException e) {
            logText("Error writing file: " + e.getMessage());
            logText(Arrays.toString(e.getStackTrace()));
        }
    }

    Region createContent() {
        var root = new BorderPane();
        root.getStylesheets().add(Objects.requireNonNull(this.getClass().getResource("/css/main.css")).toExternalForm());
        root.setTop(createMenuBar());

        var currentFileLabel = new Label("Current File:");
        currentFileLabel.getStyleClass().add("currentfile-label");
        var currentFileLabelVbox = new VBox(currentFileLabel);
        currentFileLabelVbox.setAlignment(Pos.CENTER_RIGHT);
        currentFileContent = new Label("");
        currentFileContent.getStyleClass().add("currentfile-content");
        var currentFileRow = new HBox(currentFileLabelVbox, currentFileContent);
        currentFileRow.setSpacing(10);

        var windowCountLabel = new Label("Window Count:");
        windowCountLabel.getStyleClass().add("windowcount-label");
        var windowCountLabelVbox = new VBox(windowCountLabel);
        windowCountLabelVbox.setAlignment(Pos.CENTER_RIGHT);
        windowCountTextField = new TextField("");
        windowCountTextField.getStyleClass().add("windowcount-textfield");
        windowCountTextField.setMaxWidth(50);
        var windowCountRow = new HBox(windowCountLabelVbox, windowCountTextField);
        windowCountRow.setSpacing(10);

        log = new TextArea("Loading...");
        log.getStyleClass().add("log");
        var content = new VBox(currentFileRow, windowCountRow, log);
        content.getStyleClass().add("manager");
        content.setPrefWidth(600);
        content.setPrefHeight(200);
        root.setCenter(content);
        return root;
    }

    private Region createMenuBar() {
        var fileMenu = new Menu("File");
        var newMenuItem = new MenuItem("New...");
        newMenuItem.setOnAction(_ -> handleNewAction());
        var openMenuItem = new MenuItem("Open...");
        openMenuItem.setOnAction(_ -> handleOpenAction());
        openRecentMenu = new Menu("Open Recent");
        updateOpenRecentMenu();
        var saveMenuItem = new MenuItem("Save");
        saveMenuItem.setOnAction(_ -> handleSaveAction());
        var saveAsMenuItem = new MenuItem("Save As...");
        saveAsMenuItem.setOnAction(_ -> handleSaveAsAction());
        var exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(_ -> handleExitAction());
        fileMenu.getItems().addAll(newMenuItem, openMenuItem, openRecentMenu, saveMenuItem, saveAsMenuItem, exitMenuItem);

        var actionMenu = new Menu("Action");
        var launchMenuItem = new MenuItem("Launch");
        launchMenuItem.setOnAction(_ -> handleLaunchAction());
        var killMenuItem = new MenuItem("Kill");
        killMenuItem.setOnAction(_ -> handleKillAction());
    actionMenu.getItems().addAll(launchMenuItem, killMenuItem);

        var helpMenu = new Menu("Help");
    var helpMenuItem = getHelpMenuItem("Help", "Sticky Image Manager Help", """
        This is a simple application that allows you to manage and launch multiple instances of a Viewer.
        To use this application, create a new config file or open an existing one.
        After loading a config file, set the number of instances in the 'Window Count' text field and
        click 'Launch' to start the Viewer instances. Use 'Kill' to close all instances. Save the config
        file with 'Save' or 'Save As...', and access recent files via 'Open Recent'.
        Animated GIF image playback is supported.
        Viewers run in undecorated mode: move by dragging the window, resize by dragging edges/corners.
        Exit the application with 'Exit'.
        """);
        helpMenu.getItems().add(helpMenuItem);

        var javaVersion = System.getProperty("java.version");
        var javafxVersion = System.getProperty("javafx.version");
        var aboutMenuItem = getHelpMenuItem("About", "Sticky Image Manager", String.format("""
                Version 1.0

                Sticky Image Manager is a simple application that allows you to manage and launch multiple instances of a Viewer window.

                Developed by Thomas Johnson, and written in Java using JavaFX.

                Java version: %s
                JavaFX version: %s
                """, javaVersion, javafxVersion));
        helpMenu.getItems().add(aboutMenuItem);

        var menuBar = new MenuBar(fileMenu, actionMenu, helpMenu);
        menuBar.getStyleClass().add("menu-bar");
        return menuBar;
    }

    // Decorated mode removed: always use UNDECORATED

    private static MenuItem getHelpMenuItem(String Help, String Sticky_Image_Manager_Help, String contentText) {
        var helpMenuItem = new MenuItem(Help);
        helpMenuItem.setOnAction(_ -> {
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Help);
            alert.setHeaderText(Sticky_Image_Manager_Help);
            alert.setContentText(contentText);
            alert.showAndWait();
        });
        return helpMenuItem;
    }

    private void updateOpenRecentMenu() {
        if (openRecentMenu == null) {
            return;
        }
        openRecentMenu.getItems().clear();
        for (String file : recentFiles) {
            var menuItem = new MenuItem(file);
            menuItem.setOnAction(_ -> handleOpenRecentFile(file));
            openRecentMenu.getItems().add(menuItem);
        }
    }

    private void loadFileInfo(String filename) {
      this.filename = filename;
      // Use File to handle both Unix and Windows path separators
      File file = new File(filename);
      File parent = file.getParentFile();
      String displayFilename;
      if (parent != null) {
        displayFilename = parent.getName() + File.separator + file.getName();
      } else {
        displayFilename = file.getName();
      }
      currentFileContent.setText(displayFilename);
    }

    private void handleNewAction() {
        handleKillAction();
        windowCountTextField.setText("1");
        logText("Creating new file...");
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Select config file");
        fileChooser.setInitialFileName("Config.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));

        var selectedFile = fileChooser.showSaveDialog(null);
        if (selectedFile != null) {
            logText("Selected file: " + selectedFile.getAbsolutePath());
            loadFileInfo(selectedFile.getAbsolutePath());
            loadFilePrefs(selectedFile.getAbsolutePath(), false);
        }
    }

    private void handleOpenAction() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Select config file");
        fileChooser.setInitialFileName("Config.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));

        var selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            logText("Selected file: " + selectedFile.getAbsolutePath());
            loadFileInfo(selectedFile.getAbsolutePath());
            loadFilePrefs(selectedFile.getAbsolutePath(), false);
        }
    }

    void logText(String text) {
        if (log == null) {
            return;
        }
        log.setText(text + "\n" + log.getText());
    }

    private void loadFilePrefs(String prefsFilePath, boolean isReload) {
        try {
            // If the file does not exist, assume we are creating a new file and init with default values
            if (!new File(prefsFilePath).exists()) {
                logText("File does not exist, creating new file: " + prefsFilePath);
                managerPrefs = new ManagerPrefs();
                managerPrefs.setInstanceCount(1);
                managerPrefs.setLocationX((int) stage.getX());
                managerPrefs.setLocationY((int) stage.getY());
                managerPrefs.setViewerPrefList(new ArrayList<>());
                return;
            }

            var mapper = new ObjectMapper();
            mapper.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true);
            managerPrefs = mapper.readValue(new File(prefsFilePath), ManagerPrefs.class);
            windowCountTextField.setText(String.valueOf(managerPrefs.getInstanceCount()));
            stage.setX(managerPrefs.getLocationX());
            stage.setY(managerPrefs.getLocationY());
            if (!isReload) {
                recentFiles.remove(prefsFilePath);
                recentFiles.addFirst(prefsFilePath);
                updateOpenRecentMenu();
            }
        } catch (JsonProcessingException e) {
            logText("Error processing JSON: " + e.getMessage());
            logText(Arrays.toString(e.getStackTrace()));
        } catch (IOException e) {
            logText("Error reading file: " + e.getMessage());
            logText(Arrays.toString(e.getStackTrace()));
        }
    }

    protected void handleOpenRecentFile(String file) {
        handleKillAction();
        // Check if file exists
        if (!new File(file).exists()) {
            logText("File not found: " + file);
            recentFiles.remove(file);
            saveAppPreferences();
            return;
        }

        loadFileInfo(file);
        loadFilePrefs(file, true);
        saveAppPreferences();
    }

    private void handleSaveAction() {
        try {
            if (viewerControllers.isEmpty()) {
                logText("No instances to save");
                return;
            }
            if (managerPrefs == null) {
                logText("Please select new or open a config file.");
                return;
            }

            var mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            var prefsFilePath = filename;
            managerPrefs.setInstanceCount(Integer.parseInt(windowCountTextField.getText()));
            managerPrefs.setLocationX((int) stage.getX());
            managerPrefs.setLocationY((int) stage.getY());
            managerPrefs.setViewerPrefList(viewerControllers.stream().map(ViewerController::getViewerPrefs).toList());
            mapper.writeValue(new File(prefsFilePath), managerPrefs);
            logText("Saved to file: " + prefsFilePath);

            recentFiles.remove(prefsFilePath);
            recentFiles.addFirst(prefsFilePath);
            updateOpenRecentMenu();
            saveAppPreferences();
            logText("Saved app preferences to file: " + appSettingsPath);
        } catch (JsonProcessingException e) {
            logText("Error processing JSON: " + e.getMessage());
            logText(Arrays.toString(e.getStackTrace()));
        } catch (IOException e) {
            logText("Error writing file: " + e.getMessage());
            logText(Arrays.toString(e.getStackTrace()));
        }
    }

    private void handleSaveAsAction() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Save config file");
        fileChooser.setInitialFileName("Config.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));

        var selectedFile = fileChooser.showSaveDialog(null);
        if (selectedFile != null) {
            logText("Selected file: " + selectedFile.getAbsolutePath());
            loadFileInfo(selectedFile.getAbsolutePath());
            handleSaveAction();
        }
    }

    private void handleExitAction() {
        handleKillAction();

        // Quit application
        System.exit(0);
    }

    // Expose current stage style for viewers to tailor behavior (e.g., undecorated move/resize)
    public StageStyle getCurrentStageStyle() {
        return currentStageStyle;
    }


    private void handleLaunchAction() {
        if (managerPrefs == null) {
            logText("Please select new or open a config file.");
            return;
        }
        viewerControllers = new ArrayList<>();
        var instanceCount = 0;
        try {
            instanceCount = Integer.parseInt(windowCountTextField.getText());
        } catch (NumberFormatException e) {
            logText("Invalid window count: " + windowCountTextField.getText());
            return;
        }
        for (int i = 1; i <= instanceCount; i++) {
            ViewerPrefs viewerPrefs;
            try {
                viewerPrefs = managerPrefs.getViewerPrefList().get(i - 1);
            }
            catch (NullPointerException | IndexOutOfBoundsException e) {
                viewerPrefs = new ViewerPrefs();
            }
            var viewerController = new ViewerController();
            viewerController.setParent(this);
            viewerController.setStage(new Stage());
            var content = viewerController.createContent(viewerPrefs);
            logText("Launching instance " + i + " with image path: " + viewerPrefs.getImagePath());
            var scene = new Scene(content);
            viewerController.getStage().setTitle("Sticky Image Viewer " + i);
            viewerController.getStage().setScene(scene);
            viewerController.getStage().setMaxWidth(viewerPrefs.getSizeW());
            int adjustedHeight = viewerPrefs.getSizeH();
            viewerController.getStage().setMaxHeight(Math.max(0, adjustedHeight));
            // Always use undecorated style
            currentStageStyle = StageStyle.UNDECORATED;
            viewerController.getStage().initStyle(currentStageStyle);
            viewerController.getStage().show();

            // Position using a consistent per-viewer calculation; do not mutate stored prefs
            viewerController.safeMove(
                new Point2D(viewerPrefs.getLocationX(), viewerPrefs.getLocationY()),
                new Dimension2D(viewerPrefs.getSizeW(), Math.max(0, adjustedHeight))
            );
            viewerController.getStage().setMaxWidth(Double.MAX_VALUE);
            viewerController.getStage().setMaxHeight(Double.MAX_VALUE);

            // If no image path is set, move to default location and set default size
            if (viewerPrefs.getImagePath() == null) {
            int defaultH = 300;
            viewerController.safeMove(
                new Point2D(managerPrefs.getLocationX(), managerPrefs.getLocationY()),
                new Dimension2D(300, Math.max(0, defaultH))
            );
            }
            viewerControllers.add(viewerController);
        }
    }

    private void handleKillAction() {
        for (var viewerController : viewerControllers) {
            viewerController.getStage().close();
        }
        viewerControllers.clear();
    }

    public AppPreferences loadAppPrefs(String appSettingsPath) {
        try {
            var mapper = new ObjectMapper();
            mapper.readValue(new File(appSettingsPath), AppPreferences.class);
            setAppPreferences(mapper.readValue(new File(appSettingsPath), AppPreferences.class), appSettingsPath);
        } catch (JsonProcessingException e) {
            logText("Error processing JSON: " + e.getMessage());
            logText(Arrays.toString(e.getStackTrace()));
            setAppPreferences(new AppPreferences(), appSettingsPath);
        } catch (IOException e) {
            logText("Error reading file: " + e.getMessage());
            logText(Arrays.toString(e.getStackTrace()));
            setAppPreferences(new AppPreferences(), appSettingsPath);
        }
        return appPreferences;
    }
}
