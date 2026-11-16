# syntax=docker/dockerfile:1

# ─── Build stage ──────────────────────────────────────────────────────────────
# Compiles the project and produces the runnable Spring Boot jar.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

# The cache mount keeps downloaded dependencies between builds, so only the
# first build pays for the full download (Maven Central plus JitPack, which
# hosts jaudiotagger).
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests

# ─── Runtime stage ────────────────────────────────────────────────────────────
# A JRE-only image: no Maven, no sources, no compiler.
FROM eclipse-temurin:25-jre

# Run as an unprivileged user. /data holds everything the app writes (database
# and browser uploads); /music is where a host music folder is mounted.
RUN groupadd --gid 10001 app \
 && useradd --uid 10001 --gid app --home-dir /home/app --create-home --shell /usr/sbin/nologin app \
 && mkdir -p /app /data/db /data/uploads /music \
 && chown -R app:app /app /data

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/MusicDashboardWeb-*.jar /app/app.jar

# Everything the container needs is set here, so the image runs the same
# whether or not the git-ignored application*.properties files were present
# at build time. Environment variables take precedence over those files.
#
# The database is a single-process H2 file under /data, so AUTO_SERVER (used
# when the desktop and web editions share one file) is not needed.
ENV SERVER_PORT=8080 \
    SPRING_DATASOURCE_URL=jdbc:h2:file:/data/db/musicdashboard \
    SPRING_DATASOURCE_USERNAME=sa \
    SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
    SPRING_FLYWAY_ENABLED=true \
    SPRING_FLYWAY_BASELINE_ON_MIGRATE=true \
    SPRING_FLYWAY_LOCATIONS=classpath:db/migration/common,classpath:db/migration/h2 \
    SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
    SPRING_JPA_OPEN_IN_VIEW=true \
    SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=500MB \
    SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=4GB \
    MUSICDASHBOARD_UPLOAD_DIR=/data/uploads

USER app
EXPOSE 8080
VOLUME ["/data"]

# The JRE image ships neither curl nor wget, so the check speaks HTTP through
# bash's built-in /dev/tcp: healthy when the public sign-in page answers 200.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT} \
        && printf "GET /login HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 \
        && head -n 1 <&3 | grep -q " 200"'

# JVM flags can be passed with the standard JAVA_TOOL_OPTIONS variable,
# e.g. -e JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75".
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
