package com.darkmusic.stickyimagemgr;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationViewerInstanceTest {
    @Test
    void serializedLaunchesBindMoveSaveAndCloseDistinctNewWindows() throws Exception {
        var backend = new SequencedBackend();
        var manager = new ManagerController();
        var first = new ApplicationViewerInstance(manager, prefs(100, 200), backend);
        var second = new ApplicationViewerInstance(manager, prefs(300, 400), backend);

        first.launch(1);
        second.launch(2);

        assertTrue(backend.moved.await(3, TimeUnit.SECONDS));
        assertEquals(2, backend.moves.size());
        assertEquals(2, backend.moves.stream().map(Move::window).distinct().count());
        assertTrue(backend.moves.contains(new Move(new NativeWindow("20"), new Point2D(100, 200))));
        assertTrue(backend.moves.contains(new Move(new NativeWindow("30"), new Point2D(300, 400))));

        var firstSaved = first.getViewerPrefs();
        var secondSaved = second.getViewerPrefs();
        assertEquals(100, firstSaved.getLocationX());
        assertEquals(200, firstSaved.getLocationY());
        assertEquals(300, secondSaved.getLocationX());
        assertEquals(400, secondSaved.getLocationY());

        first.kill();
        second.kill();
        assertEquals(Set.of(new NativeWindow("20"), new NativeWindow("30")), Set.copyOf(backend.closed));
    }

    private ViewerPrefs prefs(int x, int y) {
        var prefs = new ViewerPrefs();
        prefs.setType(ViewerPrefs.TYPE_APPLICATION);
        prefs.setCommand("/bin/true");
        prefs.setArguments(List.of("--start-ssb", "{test-app}"));
        prefs.setLocationX(x);
        prefs.setLocationY(y);
        prefs.setSizeW(800);
        prefs.setSizeH(600);
        return prefs;
    }

    private static final class SequencedBackend implements NativeWindowBackend {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch moved = new CountDownLatch(2);
        private final List<Move> moves = new CopyOnWriteArrayList<>();
        private final List<NativeWindow> closed = new CopyOnWriteArrayList<>();
        private final Map<NativeWindow, WinPrefs> geometries = new ConcurrentHashMap<>();

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "test";
        }

        @Override
        public List<NativeWindow> findWindows(ViewerPrefs prefs) {
            return switch (calls.incrementAndGet()) {
                case 1 -> windows("10");
                case 2, 3 -> windows("10", "20");
                default -> windows("10", "20", "30");
            };
        }

        private List<NativeWindow> windows(String... ids) {
            var result = new ArrayList<NativeWindow>();
            for (var id : ids) {
                result.add(new NativeWindow(id));
            }
            return result;
        }

        @Override
        public void moveResize(NativeWindow window, Point2D location, Dimension2D size) {
            moves.add(new Move(window, location));
            var geometry = new WinPrefs();
            geometry.setLocationX((int) location.getX());
            geometry.setLocationY((int) location.getY());
            geometry.setSizeW((int) size.getWidth());
            geometry.setSizeH((int) size.getHeight());
            geometries.put(window, geometry);
            moved.countDown();
        }

        @Override
        public void closeWindow(NativeWindow window) {
            closed.add(window);
            geometries.remove(window);
        }

        @Override
        public Optional<WinPrefs> getGeometry(NativeWindow window) {
            return Optional.ofNullable(geometries.get(window));
        }
    }

    private record Move(NativeWindow window, Point2D location) {
    }
}
