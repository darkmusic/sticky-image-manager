set windows-shell := ["pwsh", "-c"]
set shell := ["bash", "-c"]
export JAVA_HOME := "/opt/graalvm-jdk-25.0.1+8.1"

build:
  @./mvnw clean compile

install:
  @./mvnw clean install

run: install
  @./mvnw javafx:run
