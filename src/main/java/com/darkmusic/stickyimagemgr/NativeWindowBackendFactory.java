package com.darkmusic.stickyimagemgr;

class NativeWindowBackendFactory {
    private NativeWindowBackendFactory() {
    }

    static NativeWindowBackend create() {
        var i3Backend = new I3NativeWindowBackend();
        if (i3Backend.isAvailable()) {
            return i3Backend;
        }
        return new UnsupportedNativeWindowBackend();
    }
}
