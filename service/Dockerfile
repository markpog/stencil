FROM clojure:openjdk-8-lein-2.9.3-slim-buster AS build-env
WORKDIR /usr/src/myapp

COPY project.clj /usr/src/myapp/
RUN lein deps

COPY . /usr/src/myapp

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" myapp-standalone.jar

FROM openjdk:16-jdk-alpine AS java
RUN apk add --no-cache binutils
RUN  jlink -G --add-modules java.base --output /opt/java --strip-debug
        #\        && strip -p --strip-unneeded /opt/java/lib/server/libjvm.so

# FROM ubuntu:cosmic
FROM alpine:latest

ENV STENCIL_HTTP_PORT 8080
ENV STENCIL_TEMPLATE_DIR /templates
ENV STENCIL_JAVA_OPTIONS -XX:+PrintFlagsFinal

ENV JAVA_HOME=/opt/java
ENV PATH="$PATH:$JAVA_HOME/bin"

VOLUME /templates

WORKDIR /myapp
COPY --from=build-env /usr/src/myapp/myapp-standalone.jar /myapp/myapp.jar
COPY --from=java /opt/java /opt/java
# CMD [ "/opt/java/bin/java", "-jar", "/myapp/myapp.jar" ]
# RUN ls -lat /opt/java/bin
# RUN /opt/java/bin/java -version

ENTRYPOINT [ "java", "-jar",  "/myapp/myapp.jar"]
EXPOSE 8080
