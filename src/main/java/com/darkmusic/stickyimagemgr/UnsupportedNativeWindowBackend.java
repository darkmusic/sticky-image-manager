package com.darkmusic.stickyimagemgr;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

import java.util.Optional;

class UnsupportedNativeWindowBackend implements NativeWindowBackend {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return "none";
    }

    @Override
    public Optional<NativeWindow> findWindow(ViewerPrefs prefs) {
        return Optional.empty();
    }

    @Override
    public void moveResize(NativeWindow window, Point2D location, Dimension2D size) {
    }

    @Override
    public Optional<WinPrefs> getGeometry(NativeWindow window) {
        return Optional.empty();
    }
}
