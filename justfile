build:
    @./mvnw clean compile

install:
    @./mvnw clean install

run: install
    @java \
        -Dfile.encoding=UTF-8 \
        -Dsun.stdout.encoding=UTF-8 \
        -Dsun.stderr.encoding=UTF-8 \
        -classpath target/classes:\
        target/dependency/* \
        -p target/classes:target/dependency/* \
        -m com.darkmusic.stickyimagemgr/com.darkmusic.stickyimagemgr.ManagerApplication