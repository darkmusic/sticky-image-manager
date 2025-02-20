### Sticky Image Manager

This is an application that allows you to manage and launch multiple instances of a Viewer.  It is written in Java 23 and uses JavaFX for the UI.

It is designed to add some ambience to your desktop (if you have the available screen space).

Features:
- Allows individual image viewers to be resized and repositioned as desired
- Remembers positions, images, and sizes so they can be restored upon the next launch
- Supports basic EXIF rotation
- Stores recent images in a list for easy access
- Remembers the last directory when browsing for an image in the viewer's file dialog
- Supports GIF animations


![Sticky Image Manager](res/Screencast.gif)

It consists of two parts:
1. **Sticky Image Manager**: A simple application that allows you to manage and launch multiple instances of a Viewer.

*Sticky Image Manager*:

![Sticky Image Manager](res/Manager.png)

2. **Sticky Image Viewer**: A simple application that allows you to view images and videos.

*Empty Viewer (right-click to bring up context menu to allow loading / closing image)*:

![Sticky Image Viewer](res/Viewer.png)

It supports some basic EXIF rotation if present, but isn't perfect as rotated images have a bit of a white border on the left side that I haven't been able to get rid of yet.

You can resize images if needed by dragging a corner or a side.  It will maintain the aspect ratio of the image.

#### Building
To build the application, you'll need Java 23 and Maven (or you can use the mvnw wrapper).

Just open it in IntelliJ or your favorite IDE and build it.

#### Running
To run the application, build it by doing a maven install, and then run it via the IDE.

#### System Requirements
- Java 23
- OS: Windows, Linux, MacOS