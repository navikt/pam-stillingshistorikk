FROM ghcr.io/navikt/baseimages/temurin:21

ENV JAVA_OPTS="-Xms768m -Xmx1280m"

COPY build/libs/stillingshistorikk-*-all.jar ./app.jar

COPY scripts/init-env.sh /init-scripts/init-env.sh

