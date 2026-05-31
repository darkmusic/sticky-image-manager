package com.darkmusic.stickyimagemgr;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

interface ManagedViewerInstance {
    void launch(int index);

    void kill();

    void reset(Point2D location);

    ViewerPrefs getViewerPrefs();

    Dimension2D getSize();
}
