# Backend image for Render (free web service, 512 MB RAM).
# ---- build ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package && cp target/*.jar /app.jar

# ---- run ----
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 10001 --home /app app && mkdir -p /app/wallet && chown -R app /app
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 0755 /app/docker-entrypoint.sh
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    APP_HIDE_ERROR_DETAILS=true \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
