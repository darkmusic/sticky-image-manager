package com.darkmusic.stickyimagemgr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I3NativeWindowBackendTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void floorpSsbMatchesFloorpClassAndReturnsStableX11WindowIds() throws Exception {
        var tree = mapper.readTree("""
                {
                  "window": null,
                  "nodes": [{
                    "window": 101,
                    "window_properties": {"class": "floorp", "instance": "Navigator"},
                    "nodes": [],
                    "floating_nodes": []
                  }],
                  "floating_nodes": [{
                    "window": 202,
                    "window_properties": {"class": "floorp", "instance": "Navigator"},
                    "nodes": [],
                    "floating_nodes": []
                  }]
                }
                """);
        var prefs = new ViewerPrefs();
        prefs.setType(ViewerPrefs.TYPE_APPLICATION);
        prefs.setCommand("/usr/bin/floorp");
        prefs.setArguments(List.of("--start-ssb", "{4246e45b-76dd-45b3-90ef-9e6b585c091a}"));
        prefs.setWindowClass("sticky-note-app");

        var windows = new I3NativeWindowBackend().findWindows(tree, prefs);

        assertEquals(List.of(new NativeWindow("101"), new NativeWindow("202")), windows);
    }

    @Test
    void floorpFallbackDoesNotMatchOtherApplications() throws Exception {
        var tree = mapper.readTree("""
                {
                  "window": 303,
                  "window_properties": {"class": "kitty", "instance": "kitty"},
                  "nodes": [],
                  "floating_nodes": []
                }
                """);
        var prefs = new ViewerPrefs();
        prefs.setCommand("floorp");
        prefs.setArguments(List.of("--start-ssb", "{app-id}"));

        assertEquals(List.of(), new I3NativeWindowBackend().findWindows(tree, prefs));
    }
}
