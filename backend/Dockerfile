FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies
COPY src ./src
COPY config ./config
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system repairauto \
    && useradd --system --gid repairauto --home-dir /app --shell /usr/sbin/nologin repairauto \
    && mkdir -p /tmp/repairauto \
    && chown -R repairauto:repairauto /app /tmp/repairauto
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown repairauto:repairauto /app/app.jar
USER repairauto
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -Djava.io.tmpdir=/tmp/repairauto"
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["sh", "-c", "mkdir -p /tmp/repairauto && exec java -jar /app/app.jar"]
