package com.darkmusic.stickyimagemgr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class I3NativeWindowBackend implements NativeWindowBackend {
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);
    private final ObjectMapper mapper = new ObjectMapper();
    private Boolean available;

    @Override
    public boolean isAvailable() {
        if (available != null) {
            return available;
        }
        var os = System.getProperty("os.name", "").toLowerCase();
        var hasDisplay = System.getenv("DISPLAY") != null && !System.getenv("DISPLAY").isBlank();
        available = os.contains("linux") && hasDisplay && run("i3-msg", "-t", "get_version").isSuccess();
        return available;
    }

    @Override
    public String getDisplayName() {
        return "i3";
    }

    @Override
    public Optional<NativeWindow> findWindow(ViewerPrefs prefs) {
        var tree = getTree();
        if (tree.isEmpty()) {
            return Optional.empty();
        }
        var matches = new ArrayList<JsonNode>();
        collectMatchingWindows(tree.get(), prefs, matches);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NativeWindow(matches.getLast().path("id").asText()));
    }

    @Override
    public void moveResize(NativeWindow window, Point2D location, Dimension2D size) {
        var criteria = "[con_id=\"" + window.id() + "\"]";
        run("i3-msg", criteria, "floating", "enable");
        run("i3-msg", criteria, "move", "position",
                String.valueOf((int) location.getX()), String.valueOf((int) location.getY()));
        run("i3-msg", criteria, "resize", "set",
                String.valueOf((int) size.getWidth()), String.valueOf((int) size.getHeight()));
    }

    @Override
    public void closeWindow(NativeWindow window) {
        run("i3-msg", "[con_id=\"" + window.id() + "\"]", "kill");
    }

    @Override
    public Optional<WinPrefs> getGeometry(NativeWindow window) {
        var tree = getTree();
        if (tree.isEmpty()) {
            return Optional.empty();
        }
        return findById(tree.get(), window.id()).map(node -> {
            var rect = node.path("rect");
            var prefs = new WinPrefs();
            prefs.setLocationX(rect.path("x").asInt());
            prefs.setLocationY(rect.path("y").asInt());
            prefs.setSizeW(rect.path("width").asInt());
            prefs.setSizeH(rect.path("height").asInt());
            return prefs;
        });
    }

    private Optional<JsonNode> getTree() {
        var result = run("i3-msg", "-t", "get_tree");
        if (!result.isSuccess()) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readTree(result.output()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void collectMatchingWindows(JsonNode node, ViewerPrefs prefs, List<JsonNode> matches) {
        if (matchesWindow(node, prefs)) {
            matches.add(node);
        }
        collectMatchingChildren(node.path("nodes"), prefs, matches);
        collectMatchingChildren(node.path("floating_nodes"), prefs, matches);
    }

    private void collectMatchingChildren(JsonNode children, ViewerPrefs prefs, List<JsonNode> matches) {
        if (!children.isArray()) {
            return;
        }
        for (var child : children) {
            collectMatchingWindows(child, prefs, matches);
        }
    }

    private boolean matchesWindow(JsonNode node, ViewerPrefs prefs) {
        if (node.path("window").isMissingNode() || node.path("window").isNull()) {
            return false;
        }
        var expectedClass = prefs.getWindowClass();
        if (expectedClass != null && !expectedClass.isBlank()) {
            var windowClass = node.path("window_properties").path("class").asText("");
            var instance = node.path("window_properties").path("instance").asText("");
            if (expectedClass.equals(windowClass) || expectedClass.equals(instance)) {
                return true;
            }
        }
        if (matchesStartSsbWindow(node, prefs)) {
            return true;
        }
        var expectedTitle = prefs.getWindowTitle();
        if (expectedTitle != null && !expectedTitle.isBlank()) {
            return node.path("name").asText("").contains(expectedTitle);
        }
        var launchTitle = findArgumentValue(prefs.getArguments(), "-title");
        if (launchTitle.isPresent()) {
            var windowTitle = node.path("window_properties").path("title").asText(node.path("name").asText(""));
            return launchTitle.get().equals(windowTitle);
        }
        return false;
    }

    private boolean matchesStartSsbWindow(JsonNode node, ViewerPrefs prefs) {
        var arguments = prefs.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (!"--start-ssb".equals(arguments.get(i)) || i + 1 >= arguments.size()) {
                continue;
            }
            var appId = arguments.get(i + 1);
            if (appId == null || appId.isBlank()) {
                continue;
            }
            appId = appId.replace("{", "").replace("}", "");
            var windowClass = node.path("window_properties").path("class").asText("");
            return windowClass.contains(appId);
        }
        return false;
    }

    private Optional<String> findArgumentValue(List<String> arguments, String argumentName) {
        for (int i = 0; i < arguments.size() - 1; i++) {
            if (argumentName.equals(arguments.get(i))) {
                return Optional.ofNullable(arguments.get(i + 1));
            }
        }
        return Optional.empty();
    }

    private Optional<JsonNode> findById(JsonNode node, String id) {
        if (id.equals(node.path("id").asText())) {
            return Optional.of(node);
        }
        var childMatch = findByIdInChildren(node.path("nodes"), id);
        if (childMatch.isPresent()) {
            return childMatch;
        }
        return findByIdInChildren(node.path("floating_nodes"), id);
    }

    private Optional<JsonNode> findByIdInChildren(JsonNode children, String id) {
        if (!children.isArray()) {
            return Optional.empty();
        }
        for (var child : children) {
            var match = findById(child, id);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private CommandResult run(String... command) {
        try {
            var process = new ProcessBuilder(command).redirectErrorStream(true).start();
            var outputTask = new FutureTask<>(() -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            Thread.ofVirtual().name("i3-command-output").start(outputTask);
            var completed = process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
                return new CommandResult(false, "");
            }
            var output = outputTask.get(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return new CommandResult(process.exitValue() == 0, output);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new CommandResult(false, "");
        }
    }

    private record CommandResult(boolean isSuccess, String output) {
    }
}
