FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
ARG SERVICE
RUN test -n "$SERVICE" || { echo "Pass --build-arg SERVICE=<folder>"; exit 1; }
RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew ":${SERVICE}:bootJar" --no-daemon --quiet

RUN java -Djarmode=tools -jar "${SERVICE}/build/libs/${SERVICE}.jar" extract --layers --launcher --destination /extracted

FROM eclipse-temurin:25-jre
RUN apt-get -o Acquire::Retries=5 update && apt-get -o Acquire::Retries=5 install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
      && useradd --system --uid 10001 spring
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER spring
ARG PORT

RUN test -n "$PORT" || { echo "Pass --build-arg PORT=<port>"; exit 1; }
ENV PORT=${PORT}
EXPOSE ${PORT}
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
      CMD curl -fsS "http://localhost:${PORT}/actuator/health/readiness" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]