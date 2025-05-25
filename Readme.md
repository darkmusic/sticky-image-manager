### Sticky Image Manager

[![Maven Package](https://github.com/darkmusic/sticky-image-manager/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/darkmusic/sticky-image-manager/actions/workflows/maven-publish.yml)[![Java CI with Maven](https://github.com/darkmusic/sticky-image-manager/actions/workflows/maven.yml/badge.svg)](https://github.com/darkmusic/sticky-image-manager/actions/workflows/maven.yml)

This is an application that allows you to manage and launch multiple instances of a Viewer.  It is written in Java 24 and uses JavaFX for the UI.

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

1. **Sticky Image Viewer**: A simple application that allows you to view images and videos.

*Empty Viewer (right-click to bring up context menu to allow loading / closing image)*:

![Sticky Image Viewer](res/Viewer.png)

It supports some basic EXIF rotation if present, but isn't perfect as rotated images have a bit of a white border on the left side that I haven't been able to get rid of yet.

You can resize images if needed by dragging a corner or a side.  It will maintain the aspect ratio of the image.

#### Building

1. Install JDK 23 (GraalVM-CE-23.0.2 has been confirmed to work) and Maven.
2. Make sure your JAVA_HOME is set to the correct JDK version:

   ```bash
   export JAVA_HOME=/path/to/your/jdk
   ```

   or on Windows:

   ```bash
   set JAVA_HOME=C:\path\to\your\jdk
   ```

3. Also ensure your PATH variable points to the correct Java binary. For example, on Unix-like systems:

   ```bash
   export PATH=$JAVA_HOME/bin:$PATH
   ```

   On Windows:

   ```bash
   set PATH=%JAVA_HOME%\bin;%PATH%
   ```

4. In the project root, run:

   ```bash
   just build
   ```

#### Running

```bash
just run
```

#### System Requirements

- Java Runtime, currently tested with Java 24 (GraalVM-24.0.1+9.1)
- OS: Windows, Linux, MacOS
- Just CLI tool (for building and running)
