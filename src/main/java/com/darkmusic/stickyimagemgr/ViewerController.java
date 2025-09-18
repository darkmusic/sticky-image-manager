package com.darkmusic.stickyimagemgr;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifDirectoryBase;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.stage.StageStyle;
import java.io.File;

public class ViewerController {
    private ViewerPrefs viewerPrefs;
    private ContextMenu imageMenu;
    private Image image;
    private ImageView imageView;
    private Stage stage;
    private BorderPane root;
    private StackPane imageContainer;
    private ManagerController parent;
    private double imgAspect = -1; // width/height of displayed image (after EXIF rotation)
    private boolean rotatedSwap = false; // legacy flag; rotation now baked into pixels
    private boolean exifApplied = false;
    private Boolean widthConstrained = null; // legacy, not used with dual-fit binding

    // Undecorated move/resize state
    private static final double EDGE = 6.0; // px edge hit area
    private enum ResizeDir { NONE, N, S, E, W, NE, NW, SE, SW }
    private ResizeDir activeResize = ResizeDir.NONE;
    private boolean moving = false;
    private double pressScreenX, pressScreenY;
    private double pressStageX, pressStageY, pressStageW, pressStageH;
    private double dragOffsetX, dragOffsetY; // for moving

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
                    // Reuse same scene/handlers; just swap the image
                    handleImageOpen(file.getAbsolutePath(), false);
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

        // Enable custom move/resize in undecorated mode
        if (parent != null && parent.getCurrentStageStyle() == StageStyle.UNDECORATED) {
            installUndecoratedMoveResizeHandlers();
        }

        return root;
    }

    void safeMove(Point2D newLocation, Dimension2D newSize) {
        if (viewerPrefs == null) return;
        stage.setX(newLocation.getX());
        stage.setY(newLocation.getY());
        stage.setWidth(newSize.getWidth());
        stage.setHeight(newSize.getHeight());
    }

    private ChangeListener<Number> getNumberChangeListener(Dimension2D newSize, double oldImageWidth, double oldImageHeight) { return null; }

    private void handleImageOpen(String filePath, boolean setStage) {
        // Load image and synchronously bake EXIF rotation
        Image base;
        if (filePath == null) {
            base = getDefaultImage();
        } else {
            base = new Image("file:" + filePath);
        }
        Image display = base;
        try {
            int degrees = 0;
            if (filePath != null) {
                Metadata metadata = ImageMetadataReader.readMetadata(new File(filePath));
                ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (directory != null && directory.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
                    int orientation = directory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
                    switch (orientation) {
                        case 6 -> degrees = 90;   // 90 CW
                        case 3 -> degrees = 180;  // 180
                        case 8 -> degrees = 270;  // 90 CCW
                        default -> degrees = 0;
                    }
                }
            }
            if (degrees != 0) {
                display = rotateImage(base, degrees);
                rotatedSwap = (degrees == 90 || degrees == 270);
            } else {
                rotatedSwap = false;
            }
            imgAspect = display.getWidth() / display.getHeight();
        } catch (Exception _) {
            imgAspect = base.getWidth() / base.getHeight();
        }

        // Create view stack once; reuse on subsequent opens
        if (imageView == null) {
            imageView = new ImageView(display);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);
            imageView.setScaleX(1.0);
            imageView.setScaleY(1.0);
            imageView.setRotate(0);

            imageContainer = new StackPane(imageView);

            root = new BorderPane();
            root.setMinSize(0, 0);
            root.setCenter(imageContainer);
            imageContainer.setMinSize(0, 0);
            imageContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(imageContainer.widthProperty());
            clip.heightProperty().bind(imageContainer.heightProperty());
            imageContainer.setClip(clip);
            BorderPane.setAlignment(imageContainer, javafx.geometry.Pos.CENTER);
            root.setOnContextMenuRequested(event -> imageMenu.show(imageView, event.getScreenX(), event.getScreenY()));

            if (setStage) {
                Scene scene = new Scene(root);
                stage.setScene(scene);
            }
            installContainFitHandlers();
            Platform.runLater(this::updateFitBindings);
            if (stage != null) {
                stage.widthProperty().addListener((obs, o, n) -> updateFitBindings());
                stage.heightProperty().addListener((obs, o, n) -> updateFitBindings());
            }
            // Auto-fit window to image to remove initial letterboxing on load
            Platform.runLater(this::autoFitStageToImage);
        } else {
            // Reuse existing nodes/handlers; just swap the image
            imageView.setImage(display);
            imageView.setRotate(0);
            Platform.runLater(() -> {
                updateFitBindings();
                autoFitStageToImage();
            });
        }
        viewerPrefs.setImagePath(filePath);
    }

    private void autoFitStageToImage() {
        try {
            if (stage == null || imageView == null || imageView.getImage() == null) return;
            double iw = imageView.getImage().getWidth();
            double ih = imageView.getImage().getHeight();
            if (iw <= 0 || ih <= 0) return;
            double aspect = iw / ih;

            double cw = imageContainer.getWidth();
            double ch = imageContainer.getHeight();
            if (cw <= 0 || ch <= 0) {
                Platform.runLater(this::autoFitStageToImage);
                return;
            }

            // Compute ideal dimension changes but never increase size; shrink only
            double wFromH = ch * aspect;  // width that would match current height
            double hFromW = cw / aspect;  // height that would match current width

            boolean canShrinkWidth = wFromH < cw - 0.5;  // threshold to avoid jitter
            boolean canShrinkHeight = hFromW < ch - 0.5;

            if (!canShrinkWidth && !canShrinkHeight) {
                return; // would require growth → keep current size
            }

            // Prefer the smaller change
            double dw = canShrinkWidth ? (cw - wFromH) : Double.MAX_VALUE;
            double dh = canShrinkHeight ? (ch - hFromW) : Double.MAX_VALUE;

            double newW = cw;
            double newH = ch;
            if (dw <= dh) {
                newW = Math.max(100, wFromH);
            } else {
                newH = Math.max(100, hFromW);
            }

            stage.setWidth(newW);
            stage.setHeight(newH);
        } catch (Exception ignored) {
        }
    }
    private Image rotateImage(Image src, int degrees) {
        PixelReader reader = src.getPixelReader();
        int w = (int) Math.round(src.getWidth());
        int h = (int) Math.round(src.getHeight());
        if (reader == null || w <= 0 || h <= 0) return src;

        int norm = ((degrees % 360) + 360) % 360;
        if (norm == 0) return src;

        switch (norm) {
            case 90: {
                WritableImage out = new WritableImage(h, w);
                PixelWriter pw = out.getPixelWriter();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = reader.getArgb(x, y);
                        int nx = h - 1 - y;
                        int ny = x;
                        pw.setArgb(nx, ny, argb);
                    }
                }
                return out;
            }
            case 180: {
                WritableImage out = new WritableImage(w, h);
                PixelWriter pw = out.getPixelWriter();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = reader.getArgb(x, y);
                        int nx = w - 1 - x;
                        int ny = h - 1 - y;
                        pw.setArgb(nx, ny, argb);
                    }
                }
                return out;
            }
            case 270: {
                WritableImage out = new WritableImage(h, w);
                PixelWriter pw = out.getPixelWriter();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = reader.getArgb(x, y);
                        int nx = y;
                        int ny = w - 1 - x;
                        pw.setArgb(nx, ny, argb);
                    }
                }
                return out;
            }
            default:
                return src;
        }
    }

    private void installContainFitHandlers() {
        imageView.setPreserveRatio(true);
        // Robust contain: bind both fits to container; preserveRatio ensures the smaller axis is used
        imageView.fitWidthProperty().unbind();
        imageView.fitHeightProperty().unbind();
        imageView.fitWidthProperty().bind(imageContainer.widthProperty());
        imageView.fitHeightProperty().bind(imageContainer.heightProperty());
        // When image changes, nothing else needed; bindings handle sizing
    }

    private void updateFitBindings() {
        // No-op with dual-fit binding; preserveRatio decides effective fit
        imageView.setPreserveRatio(true);
    }

    private void installUndecoratedMoveResizeHandlers() {
        // Update cursor when hovering near edges
        root.setOnMouseMoved(e -> {
            if (!isPrimary(e.isPrimaryButtonDown())) {
                ResizeDir dir = hitTest(e.getX(), e.getY());
                root.setCursor(cursorFor(dir));
            }
        });

        root.setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) return;
            pressScreenX = e.getScreenX();
            pressScreenY = e.getScreenY();
            pressStageX = stage.getX();
            pressStageY = stage.getY();
            pressStageW = stage.getWidth();
            pressStageH = stage.getHeight();

            activeResize = hitTest(e.getX(), e.getY());
            moving = (activeResize == ResizeDir.NONE);
            if (moving) {
                dragOffsetX = e.getScreenX() - stage.getX();
                dragOffsetY = e.getScreenY() - stage.getY();
                root.setCursor(Cursor.MOVE);
            }
        });

        root.setOnMouseDragged(e -> {
            if (!e.isPrimaryButtonDown()) return;
            // Disable cache while actively resizing for responsive updates
            imageView.setCache(false);
            if (moving) {
                double newX = e.getScreenX() - dragOffsetX;
                double newY = e.getScreenY() - dragOffsetY;
                stage.setX(newX);
                stage.setY(newY);
            } else {
                applyResize(e.getScreenX(), e.getScreenY());
                // Update fit immediately while resizing
                updateFitBindings();
            }
            // Bound sizing updates automatically; no manual recompute needed
        });

        root.setOnMouseReleased(_e -> {
            moving = false;
            activeResize = ResizeDir.NONE;
            root.setCursor(Cursor.DEFAULT);
            // Re-enable cache after finishing resize/move
            imageView.setCache(true);
        });
    }

    private void applyResize(double screenX, double screenY) {
        double minW = 100, minH = 100; // aligns with WinPrefs
        double dx = screenX - pressScreenX;
        double dy = screenY - pressScreenY;

        double newX = pressStageX;
        double newY = pressStageY;
        double newW = pressStageW;
        double newH = pressStageH;

        double aspect = imgAspect;
        if (aspect <= 0) {
            // Fallback to current window aspect
            aspect = Math.max(0.1, pressStageW / Math.max(1.0, pressStageH));
        }

        switch (activeResize) {
            case E -> {
                newW = clamp(pressStageW + dx, minW, Double.MAX_VALUE);
                newH = Math.max(minH, newW / aspect);
            }
            case S -> {
                newH = clamp(pressStageH + dy, minH, Double.MAX_VALUE);
                newW = Math.max(minW, newH * aspect);
            }
            case W -> {
                double w = clamp(pressStageW - dx, minW, Double.MAX_VALUE);
                newX = pressStageX + (pressStageW - w);
                newW = w;
                newH = Math.max(minH, newW / aspect);
            }
            case N -> {
                double h = clamp(pressStageH - dy, minH, Double.MAX_VALUE);
                newY = pressStageY + (pressStageH - h);
                newH = h;
                newW = Math.max(minW, newH * aspect);
            }
            case SE -> {
                if (Math.abs(dx) >= Math.abs(dy)) {
                    newW = clamp(pressStageW + dx, minW, Double.MAX_VALUE);
                    newH = Math.max(minH, newW / aspect);
                } else {
                    newH = clamp(pressStageH + dy, minH, Double.MAX_VALUE);
                    newW = Math.max(minW, newH * aspect);
                }
            }
            case SW -> {
                if (Math.abs(dx) >= Math.abs(dy)) {
                    double w = clamp(pressStageW - dx, minW, Double.MAX_VALUE);
                    newX = pressStageX + (pressStageW - w);
                    newW = w;
                    newH = Math.max(minH, newW / aspect);
                } else {
                    newH = clamp(pressStageH + dy, minH, Double.MAX_VALUE);
                    newW = Math.max(minW, newH * aspect);
                    double w = newW;
                    newX = pressStageX + (pressStageW - w);
                }
            }
            case NE -> {
                if (Math.abs(dx) >= Math.abs(dy)) {
                    newW = clamp(pressStageW + dx, minW, Double.MAX_VALUE);
                    newH = Math.max(minH, newW / aspect);
                    double h = newH;
                    newY = pressStageY + (pressStageH - h);
                } else {
                    double h = clamp(pressStageH - dy, minH, Double.MAX_VALUE);
                    newY = pressStageY + (pressStageH - h);
                    newH = h;
                    newW = Math.max(minW, newH * aspect);
                }
            }
            case NW -> {
                if (Math.abs(dx) >= Math.abs(dy)) {
                    double w = clamp(pressStageW - dx, minW, Double.MAX_VALUE);
                    newX = pressStageX + (pressStageW - w);
                    newW = w;
                    newH = Math.max(minH, newW / aspect);
                    double h = newH;
                    newY = pressStageY + (pressStageH - h);
                } else {
                    double h = clamp(pressStageH - dy, minH, Double.MAX_VALUE);
                    newY = pressStageY + (pressStageH - h);
                    newH = h;
                    double w = Math.max(minW, newH * aspect);
                    newX = pressStageX + (pressStageW - w);
                    newW = w;
                }
            }
            case NONE -> { /* no-op */ }
        }

        stage.setX(newX);
        stage.setY(newY);
        stage.setWidth(newW);
        stage.setHeight(newH);
    }

    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    private ResizeDir hitTest(double x, double y) {
        double w = root.getWidth();
        double h = root.getHeight();
        boolean left = x <= EDGE;
        boolean right = x >= w - EDGE;
        boolean top = y <= EDGE;
        boolean bottom = y >= h - EDGE;
        if (top && left) return ResizeDir.NW;
        if (top && right) return ResizeDir.NE;
        if (bottom && left) return ResizeDir.SW;
        if (bottom && right) return ResizeDir.SE;
        if (top) return ResizeDir.N;
        if (bottom) return ResizeDir.S;
        if (left) return ResizeDir.W;
        if (right) return ResizeDir.E;
        return ResizeDir.NONE;
    }

    private Cursor cursorFor(ResizeDir dir) {
        return switch (dir) {
            case N -> Cursor.N_RESIZE;
            case S -> Cursor.S_RESIZE;
            case E -> Cursor.E_RESIZE;
            case W -> Cursor.W_RESIZE;
            case NE -> Cursor.NE_RESIZE;
            case NW -> Cursor.NW_RESIZE;
            case SE -> Cursor.SE_RESIZE;
            case SW -> Cursor.SW_RESIZE;
            default -> Cursor.DEFAULT;
        };
    }

    private boolean isPrimary(boolean primaryDownFlag) { return primaryDownFlag; }

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
