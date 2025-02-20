package com.darkmusic.stickyimagemgr;

import java.util.List;

public class ManagerPrefs extends WinPrefs {
    private int instanceCount;
    private List<ViewerPrefs> viewerPrefList;

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public List<ViewerPrefs> getViewerPrefList() {
        return viewerPrefList;
    }

    public void setViewerPrefList(List<ViewerPrefs> viewerPrefList) {
        this.viewerPrefList = viewerPrefList;
    }
}
