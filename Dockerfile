FROM ghcr.io/navikt/baseimages/temurin:11

COPY init.sh /init-scripts/init.sh
COPY build/libs/*.jar ./

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"