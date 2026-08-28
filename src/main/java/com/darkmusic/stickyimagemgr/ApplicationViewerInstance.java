package com.darkmusic.stickyimagemgr;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class ApplicationViewerInstance implements ManagedViewerInstance {
    private static final long WINDOW_WAIT_MILLIS = 8000;
    private static final long WINDOW_POLL_MILLIS = 100;

    private final ViewerPrefs initialPrefs;
    private final NativeWindowBackend nativeWindowBackend;
    private final ManagerController parent;
    private final Object stateLock = new Object();
    private volatile Process process;
    private volatile NativeWindow nativeWindow;
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

        if (!nativeWindowBackend.isAvailable()) {
            if (startProcess(index, command, builder)) {
                parent.logText("Application viewer " + index + " launched in launch-only mode; native window positioning is unavailable.");
            }
            return;
        }

        parent.launchApplicationViewer(() -> launchAndManageNativeWindow(index, command, builder));
    }

    @Override
    public void kill() {
        killed = true;
        if (ensureNativeWindow()) {
            nativeWindowBackend.closeWindow(nativeWindow);
            nativeWindow = null;
        }
        var launchedProcess = process;
        if (launchedProcess == null) {
            return;
        }
        launchedProcess.destroy();
        if (launchedProcess.isAlive()) {
            launchedProcess.destroyForcibly();
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
        var geometry = getCurrentGeometry();
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
        var geometry = getCurrentGeometry();
        if (geometry.isPresent()) {
            return new Dimension2D(geometry.get().getSizeW(), geometry.get().getSizeH());
        }
        return new Dimension2D(initialPrefs.getSizeW(), initialPrefs.getSizeH());
    }

    private NativeWindow waitForNewWindow(Set<NativeWindow> windowsBeforeLaunch) {
        var deadline = System.currentTimeMillis() + WINDOW_WAIT_MILLIS;
        while (!killed && System.currentTimeMillis() < deadline) {
            var windows = nativeWindowBackend.findWindows(initialPrefs);
            for (var window : windows.reversed()) {
                if (!windowsBeforeLaunch.contains(window)) {
                    return window;
                }
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

    private void launchAndManageNativeWindow(int index, ArrayList<String> command, ProcessBuilder builder) {
        // Floorp gives every SSB the same WM_CLASS. The manager runs these tasks
        // in config order, so each snapshot/launch/discovery cycle claims the
        // window created by its own command rather than another viewer's window.
        if (killed) {
            return;
        }
        var windowsBeforeLaunch = new HashSet<>(nativeWindowBackend.findWindows(initialPrefs));
        if (!startProcess(index, command, builder)) {
            return;
        }
        var window = waitForNewWindow(windowsBeforeLaunch);
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

    private boolean startProcess(int index, ArrayList<String> command, ProcessBuilder builder) {
        try {
            var launchedProcess = builder.start();
            synchronized (stateLock) {
                process = launchedProcess;
                if (killed) {
                    launchedProcess.destroyForcibly();
                    return false;
                }
            }
            parent.logText("Launching application viewer " + index + ": " + String.join(" ", command));
            return true;
        } catch (IOException e) {
            parent.logText("Failed to launch application viewer " + index + ": " + e.getMessage());
            return false;
        }
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
        // NativeWindow now stores the stable X11 window ID, not i3's container
        // ID, so floating/reparenting cannot make this reference stale.
        if (nativeWindow != null) {
            return true;
        }
        if (hasStartSsbArgument()) {
            // A Floorp SSB cannot be safely re-identified after its X11 window
            // disappears because neither i3 nor X11 exposes the SSB UUID.
            return false;
        }
        var window = nativeWindowBackend.findWindow(initialPrefs);
        if (window.isEmpty()) {
            return false;
        }
        nativeWindow = window.get();
        return true;
    }

    private Optional<WinPrefs> getCurrentGeometry() {
        if (nativeWindow != null) {
            var geometry = nativeWindowBackend.getGeometry(nativeWindow);
            if (geometry.isPresent()) {
                return geometry;
            }
            if (hasStartSsbArgument()) {
                // Keep the only safe identity through transient i3 IPC errors.
                return Optional.empty();
            }
            nativeWindow = null;
        }
        if (!ensureNativeWindow()) {
            return Optional.empty();
        }
        return nativeWindowBackend.getGeometry(nativeWindow);
    }

    private boolean hasStartSsbArgument() {
        return initialPrefs.getArguments().contains("--start-ssb");
    }
}
