package com.darkmusic.stickyimagemgr;

import java.util.ArrayList;
import java.util.List;

public class ViewerPrefs extends WinPrefs {
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_APPLICATION = "application";

    private String type = TYPE_IMAGE;
    private String imagePath;
    private String name;
    private String command;
    private List<String> arguments = new ArrayList<>();
    private String workingDirectory;
    private String windowClass;
    private String windowTitle;

    public String getType() {
        if (type == null || type.isBlank()) {
            return TYPE_IMAGE;
        }
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments == null ? new ArrayList<>() : arguments;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getWindowClass() {
        return windowClass;
    }

    public void setWindowClass(String windowClass) {
        this.windowClass = windowClass;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    public ViewerPrefs copy() {
        var copy = new ViewerPrefs();
        copy.setType(getType());
        copy.setLocationX(getLocationX());
        copy.setLocationY(getLocationY());
        copy.setSizeW(getSizeW());
        copy.setSizeH(getSizeH());
        copy.setImagePath(imagePath);
        copy.setName(name);
        copy.setCommand(command);
        copy.setArguments(new ArrayList<>(arguments));
        copy.setWorkingDirectory(workingDirectory);
        copy.setWindowClass(windowClass);
        copy.setWindowTitle(windowTitle);
        return copy;
    }
}
