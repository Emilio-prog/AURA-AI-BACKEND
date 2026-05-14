FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system aura \
    && useradd --system --gid aura --home-dir /app --shell /usr/sbin/nologin aura

COPY --from=build --chown=aura:aura /workspace/target/aura-ai-backend-*.jar app.jar

USER aura
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=5 --start-period=60s \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
