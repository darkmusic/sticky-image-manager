package com.darkmusic.stickyimagemgr;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.Optional;

interface NativeWindowBackend {
    boolean isAvailable();

    String getDisplayName();

    List<NativeWindow> findWindows(ViewerPrefs prefs);

    default Optional<NativeWindow> findWindow(ViewerPrefs prefs) {
        var windows = findWindows(prefs);
        return windows.isEmpty() ? Optional.empty() : Optional.of(windows.getLast());
    }

    void moveResize(NativeWindow window, Point2D location, Dimension2D size);

    void closeWindow(NativeWindow window);

    Optional<WinPrefs> getGeometry(NativeWindow window);
}
