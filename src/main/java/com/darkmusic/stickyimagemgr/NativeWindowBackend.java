package com.darkmusic.stickyimagemgr;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

import java.util.Optional;

interface NativeWindowBackend {
    boolean isAvailable();

    String getDisplayName();

    Optional<NativeWindow> findWindow(ViewerPrefs prefs);

    void moveResize(NativeWindow window, Point2D location, Dimension2D size);

    void closeWindow(NativeWindow window);

    Optional<WinPrefs> getGeometry(NativeWindow window);
}
