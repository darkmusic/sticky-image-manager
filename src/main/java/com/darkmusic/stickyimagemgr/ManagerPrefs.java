package com.darkmusic.stickyimagemgr;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class ManagerPrefs extends WinPrefs {
    private int instanceCount;
    private List<ViewerPrefs> instances;
    private List<ViewerPrefs> viewerPrefList;

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public List<ViewerPrefs> getInstances() {
        return instances;
    }

    public void setInstances(List<ViewerPrefs> instances) {
        this.instances = instances;
    }

    @JsonIgnore
    public List<ViewerPrefs> getViewerEntries() {
        if (instances != null) {
            return instances;
        }
        if (viewerPrefList != null) {
            return viewerPrefList;
        }
        return new ArrayList<>();
    }

    public List<ViewerPrefs> getViewerPrefList() {
        return viewerPrefList;
    }

    public void setViewerPrefList(List<ViewerPrefs> viewerPrefList) {
        this.viewerPrefList = viewerPrefList;
    }
}
