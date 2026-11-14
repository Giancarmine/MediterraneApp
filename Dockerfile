# Docker multi-stage build

# 1. Build the application with the committed Maven wrapper.
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /MediterraneApp

COPY . .

RUN ./mvnw -B -ntp -DskipTests package


# 2. Run the packaged JAR as a non-root user.
FROM eclipse-temurin:25-jre

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /MediterraneApp/target/mediterraneapp-0.0.1-SNAPSHOT.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl --fail --silent "http://localhost:${SERVER_PORT:-8080}/recipe/status" > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
