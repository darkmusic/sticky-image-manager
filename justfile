build:
    @./mvnw clean compile

install:
    @./mvnw clean install

run: install
    @./mvnw javafx:run