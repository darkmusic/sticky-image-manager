package com.darkmusic.stickyimagemgr;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class ApplicationViewerInstance implements ManagedViewerInstance {
    private static final long WINDOW_WAIT_MILLIS = 8000;
    private static final long WINDOW_POLL_MILLIS = 100;

    private final ViewerPrefs initialPrefs;
    private final NativeWindowBackend nativeWindowBackend;
    private final ManagerController parent;
    private Process process;
    private NativeWindow nativeWindow;
    private volatile boolean killed;

    ApplicationViewerInstance(ManagerController parent, ViewerPrefs initialPrefs, NativeWindowBackend nativeWindowBackend) {
        this.parent = parent;
        this.initialPrefs = initialPrefs;
        this.nativeWindowBackend = nativeWindowBackend;
    }

    @Override
    public void launch(int index) {
        if (initialPrefs.getCommand() == null || initialPrefs.getCommand().isBlank()) {
            parent.logText("Application viewer " + index + " has no command configured.");
            return;
        }

        var command = new ArrayList<String>();
        command.add(initialPrefs.getCommand());
        addStableWindowIdentityArguments(command);
        command.addAll(initialPrefs.getArguments());
        var builder = new ProcessBuilder(command);
        if (initialPrefs.getWorkingDirectory() != null && !initialPrefs.getWorkingDirectory().isBlank()) {
            builder.directory(new File(initialPrefs.getWorkingDirectory()));
        }

        try {
            process = builder.start();
            parent.logText("Launching application viewer " + index + ": " + String.join(" ", command));
        } catch (IOException e) {
            parent.logText("Failed to launch application viewer " + index + ": " + e.getMessage());
            return;
        }

        if (!nativeWindowBackend.isAvailable()) {
            parent.logText("Application viewer " + index + " launched in launch-only mode; native window positioning is unavailable.");
            return;
        }

        Thread.ofVirtual().name("application-viewer-window-" + index).start(() -> manageNativeWindow(index));
    }

    @Override
    public void kill() {
        killed = true;
        if (ensureNativeWindow()) {
            nativeWindowBackend.closeWindow(nativeWindow);
            nativeWindow = null;
        }
        if (process == null) {
            return;
        }
        process.destroy();
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    @Override
    public void reset(Point2D location) {
        if (!ensureNativeWindow()) {
            parent.logText("Application viewer reset skipped; native window positioning is unavailable.");
            return;
        }
        nativeWindowBackend.moveResize(nativeWindow, location, getSize());
    }

    @Override
    public ViewerPrefs getViewerPrefs() {
        var prefs = initialPrefs.copy();
        if (!ensureNativeWindow()) {
            parent.logText("Application viewer geometry not captured; keeping configured geometry.");
            return prefs;
        }
        var geometry = nativeWindowBackend.getGeometry(nativeWindow);
        if (geometry.isEmpty()) {
            parent.logText("Application viewer geometry not found; keeping configured geometry.");
            return prefs;
        }
        var winPrefs = geometry.get();
        prefs.setLocationX(winPrefs.getLocationX());
        prefs.setLocationY(winPrefs.getLocationY());
        prefs.setSizeW(winPrefs.getSizeW());
        prefs.setSizeH(winPrefs.getSizeH());
        return prefs;
    }

    @Override
    public Dimension2D getSize() {
        if (ensureNativeWindow()) {
            var geometry = nativeWindowBackend.getGeometry(nativeWindow);
            if (geometry.isPresent()) {
                return new Dimension2D(geometry.get().getSizeW(), geometry.get().getSizeH());
            }
        }
        return new Dimension2D(initialPrefs.getSizeW(), initialPrefs.getSizeH());
    }

    private NativeWindow waitForWindow() {
        var deadline = System.currentTimeMillis() + WINDOW_WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            var window = nativeWindowBackend.findWindow(initialPrefs);
            if (window.isPresent()) {
                return window.get();
            }
            try {
                Thread.sleep(WINDOW_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private void manageNativeWindow(int index) {
        var window = waitForWindow();
        if (killed) {
            return;
        }
        if (window == null) {
            parent.logText("Application viewer " + index + " launched, but no matching native window was found.");
            return;
        }
        nativeWindow = window;
        nativeWindowBackend.moveResize(nativeWindow,
                new Point2D(initialPrefs.getLocationX(), initialPrefs.getLocationY()),
                new Dimension2D(initialPrefs.getSizeW(), initialPrefs.getSizeH()));
    }

    private void addStableWindowIdentityArguments(ArrayList<String> command) {
        if (!"urxvt".equals(initialPrefs.getCommand()) || initialPrefs.getWindowClass() == null
                || initialPrefs.getWindowClass().isBlank() || initialPrefs.getArguments().contains("-name")) {
            return;
        }
        command.add("-name");
        command.add(initialPrefs.getWindowClass());
    }

    private boolean ensureNativeWindow() {
        if (!nativeWindowBackend.isAvailable()) {
            return false;
        }
        if (nativeWindow != null) {
            return true;
        }
        var window = nativeWindowBackend.findWindow(initialPrefs);
        if (window.isEmpty()) {
            return false;
        }
        nativeWindow = window.get();
        return true;
    }
}
