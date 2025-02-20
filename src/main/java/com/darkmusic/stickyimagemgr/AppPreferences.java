package com.darkmusic.stickyimagemgr;

import java.util.List;

public class AppPreferences {
    private List<String> recentFiles;

    public List<String> getRecentFiles() {
        return recentFiles;
    }

    public void setRecentFiles(List<String> recentFiles) {
        this.recentFiles = recentFiles;
    }
}
