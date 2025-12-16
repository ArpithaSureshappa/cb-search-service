FROM openjdk:17.0.1-jdk-slim
# Install necessary dependencies
RUN apt-get update \
    && apt-get install -y \
        curl \
        libxrender1 \
        libjpeg62-turbo \
        fontconfig \
        libxtst6 \
        xfonts-75dpi \
        xfonts-base \
        xz-utils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
COPY cb-search-service-0.0.1-SNAPSHOT.jar /opt/
#HEALTHCHECK --interval=30s --timeout=30s CMD curl --fail http://localhost:7001/actuator/health || exit 1
CMD ["/bin/bash", "-c", "java -XX:+PrintFlagsFinal $JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -jar /opt/cb-search-service-0.0.1-SNAPSHOT.jar"]
