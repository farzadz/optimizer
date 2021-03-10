FROM maven:3-jdk-11

ARG DEBIAN_FRONTEND=noninteractive
USER root

RUN apt-get update; \
    apt-get -yq --no-install-recommends install build-essential &&\
    rm -rf /var/lib/apt/lists/*;

WORKDIR /usr/lib

ENV OR_TOOLS_PATH /usr/lib/ortools/lib

ADD ["https://github.com/google/or-tools/releases/download/v7.8/or-tools_ubuntu-18.04_v7.8.7959.tar.gz", "."]

RUN tar xzf or-tools_ubuntu-18.04_v7.8.7959.tar.gz &&\
    mv or-tools_Ubuntu-18.04-64bit_v7.8.7959 ortools; \
    rm or-tools_ubuntu-18.04_v7.8.7959.tar.gz

RUN mvn install:install-file "-Dfile=${OR_TOOLS_PATH}/com.google.ortools.jar" "-DgroupId=com.google.ortools" "-DartifactId=ortools" "-Dversion=7.8" "-Dpackaging=jar" "-DgeneratePom=true"

WORKDIR /usr/src/build

COPY . /usr/src/build

RUN mvn clean verify
