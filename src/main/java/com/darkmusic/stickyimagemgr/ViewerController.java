package com.darkmusic.stickyimagemgr;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifDirectoryBase;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class ViewerController {
    private ViewerPrefs viewerPrefs;
    private ContextMenu imageMenu;
    private Image image;
    private ImageView imageView;
    private Stage stage;
    private BorderPane root;
    private ManagerController parent;

    void setParent(ManagerController parent) {
        this.parent = parent;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public ViewerPrefs getViewerPrefs() {
        var newViewerPrefs = new ViewerPrefs();
        newViewerPrefs.setLocationX((int) stage.getX());
        newViewerPrefs.setLocationY((int) stage.getY());
        newViewerPrefs.setSizeW((int) stage.getWidth());
        newViewerPrefs.setSizeH((int) stage.getHeight());
        newViewerPrefs.setImagePath(viewerPrefs.getImagePath());
        return newViewerPrefs;
    }

    Region createContent(ViewerPrefs viewerPrefs) {
        this.viewerPrefs = viewerPrefs;

        imageMenu = new ContextMenu();
        imageMenu.setAutoHide(true);
        imageMenu.getItems().add(new MenuItem("Open"));
        imageMenu.setOnAction(event -> {
            var item = (MenuItem) event.getTarget();
            var text = item.getText();
            if (text.equals("Open")) {
                var fileChooser = new FileChooser();
                fileChooser.setTitle("Open Image");
                if (parent.getLastUsedDirectory() == null) {
                    parent.setLastUsedDirectory(System.getProperty("user.home"));
                }
                fileChooser.setInitialDirectory(new File(parent.getLastUsedDirectory()));
                fileChooser.setInitialFileName("");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.PNG", "*.jpg", "*.JPG", "*.gif", "*.GIF", "*.bmp", "*.BMP", "*.jpeg", "*.JPEG"),
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                var file = fileChooser.showOpenDialog(null);
                if (file != null) {
                    handleImageOpen(file.getAbsolutePath(), true);
                    parent.setLastUsedDirectory(file.getParent());
                }
            } else if (text.equals("Close")) {
                handleImageClose();
            }
        });
        imageMenu.getItems().add(new MenuItem("Close"));

        try {
            handleImageOpen(this.viewerPrefs.getImagePath(), false);
            parent.setLastUsedDirectory(new File(this.viewerPrefs.getImagePath()).getParent());
        }
        catch (Exception e) {
            System.out.println("Error loading image: " + e.getMessage());
            image = getDefaultImage();
        }

        return root;
    }

    void safeMove(Point2D newLocation, Dimension2D newSize) {
        if (viewerPrefs == null) return;
        stage.setX(newLocation.getX());
        stage.setY(newLocation.getY());
        stage.setWidth(newSize.getWidth());
        stage.setHeight(newSize.getHeight());
        if (imageView == null) return;
        imageView.setFitHeight(newSize.getHeight());
        double oldImageWidth = newSize.getWidth(), oldImageHeight = newSize.getHeight();
        ChangeListener<Number> listener = getNumberChangeListener(newSize, oldImageWidth, oldImageHeight);
        stage.widthProperty().addListener(listener);
        stage.heightProperty().addListener(listener);
    }

    private ChangeListener<Number> getNumberChangeListener(Dimension2D newSize, double oldImageWidth, double oldImageHeight) {
        ChangeListener<Number> listener = (_, _, _) -> {
            double paneHeight = stage.getScene().getHeight();
            imageView.setFitHeight(paneHeight);
        };
        return listener;
    }

    private void handleImageOpen(String filePath, boolean setStage) {
        if (filePath == null) {
            image = getDefaultImage();
        }
        else {
            image = new Image("file:" + filePath);
        }
        imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        ChangeListener<Image> imageChangeListener = (_, _, newImage) -> {
            if (newImage != null) {
                try {
                    Metadata metadata = ImageMetadataReader.readMetadata(new File(filePath));
                    ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                    if (directory != null && directory.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
                        int orientation = directory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
                        switch (orientation) {
                            case 6: // 90 degrees CW
                                imageView.setRotate(90);
                                break;
                            case 3: // 180 degrees
                                imageView.setRotate(180);
                                break;
                            case 8: // 90 degrees CCW
                                imageView.setRotate(270);
                                break;
                            default:
                                imageView.setRotate(0);
                                break;
                        }

                        // Swap fitWidth and fitHeight for 90 and 270 degree rotations
                        if (orientation == 6 || orientation == 8) {
                            imageView.setFitWidth(newImage.getHeight());
                            imageView.setFitHeight(newImage.getWidth());
                        } else {
                            imageView.setFitWidth(newImage.getWidth());
                            imageView.setFitHeight(newImage.getHeight());
                        }
                        root.requestLayout();
                    }
                } catch (Exception _) {
                }
            }
        };

        imageView.imageProperty().addListener(imageChangeListener);

        // Manually invoke the listener if the image is already loaded
        if (image.isError() || image.getProgress() == 1.0) {
            imageChangeListener.changed(null, null, image);
        }

        root = new BorderPane();
        root.setCenter(imageView);
        BorderPane.setAlignment(imageView, javafx.geometry.Pos.CENTER); // Center the ImageView

        root.setOnContextMenuRequested(event -> imageMenu.show(imageView, event.getScreenX(), event.getScreenY()));

        if (setStage) {
            Scene scene = new Scene(root);
            stage.setScene(scene);
        }
        viewerPrefs.setImagePath(filePath);
    }

    private void handleImageClose() {
        image = getDefaultImage();
    }

    private Image getDefaultImage() {
        int width = 200;
        int height = 200;
        WritableImage defaultImage = new WritableImage(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                defaultImage.getPixelWriter().setColor(x, y, Color.LIGHTGRAY);
            }
        }
        return defaultImage;
    }

    public Stage getStage() {
        return stage;
    }
}
