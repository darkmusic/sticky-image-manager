module com.darkmusic.stickyimagemgr {
    requires javafx.controls;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.fasterxml.jackson.databind;
    requires metadata.extractor;

    opens com.darkmusic.stickyimagemgr to javafx.base;
    exports com.darkmusic.stickyimagemgr;
}