set windows-shell := ["pwsh", "-c"]
set shell := ["bash", "-c"]
#export JAVA_HOME := "C:\\Apps\\graalvm-jdk-24.0.2+11.1"

build:
  @./mvnw clean compile

install:
  @./mvnw clean install

run: install
  @./mvnw javafx:run
