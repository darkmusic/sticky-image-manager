module com.darkmusic.stickyimagemgr {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires transitive javafx.base;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.fasterxml.jackson.databind;
    requires metadata.extractor;

    opens com.darkmusic.stickyimagemgr to javafx.base;
    exports com.darkmusic.stickyimagemgr;
}