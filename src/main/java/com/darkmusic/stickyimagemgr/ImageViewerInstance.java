package com.darkmusic.stickyimagemgr;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

class ImageViewerInstance implements ManagedViewerInstance {
    private final ManagerController parent;
    private final ViewerPrefs initialPrefs;
    private ViewerController controller;

    ImageViewerInstance(ManagerController parent, ViewerPrefs initialPrefs) {
        this.parent = parent;
        this.initialPrefs = initialPrefs;
    }

    @Override
    public void launch(int index) {
        controller = new ViewerController();
        controller.setParent(parent);
        controller.setStage(new Stage());
        var content = controller.createContent(initialPrefs);
        var scene = new Scene(content);
        controller.getStage().setTitle("Sticky Image Viewer " + index);
        controller.getStage().setScene(scene);
        controller.getStage().setMaxWidth(initialPrefs.getSizeW());
        controller.getStage().setMaxHeight(initialPrefs.getSizeH());
        controller.getStage().initStyle(StageStyle.UNDECORATED);
        controller.getStage().show();

        controller.safeMove(
                new Point2D(initialPrefs.getLocationX(), initialPrefs.getLocationY()),
                new Dimension2D(initialPrefs.getSizeW(), initialPrefs.getSizeH()));
        controller.getStage().setMaxWidth(Double.MAX_VALUE);
        controller.getStage().setMaxHeight(Double.MAX_VALUE);

        if (initialPrefs.getImagePath() == null) {
            controller.safeMove(
                    new Point2D(parent.getManagerLocationX(), parent.getManagerLocationY()),
                    new Dimension2D(300, 300));
        }
    }

    @Override
    public void kill() {
        if (controller != null) {
            controller.getStage().close();
        }
    }

    @Override
    public void reset(Point2D location) {
        if (controller == null) {
            return;
        }
        controller.safeMove(location, getSize());
    }

    @Override
    public ViewerPrefs getViewerPrefs() {
        if (controller == null) {
            return initialPrefs.copy();
        }
        return controller.getViewerPrefs();
    }

    @Override
    public Dimension2D getSize() {
        if (controller == null) {
            return new Dimension2D(initialPrefs.getSizeW(), initialPrefs.getSizeH());
        }
        return new Dimension2D(controller.getStage().getWidth(), controller.getStage().getHeight());
    }
}
