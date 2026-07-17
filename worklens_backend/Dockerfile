FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system worklens \
    && useradd --system --gid worklens --home-dir /app --shell /usr/sbin/nologin worklens

COPY --from=build --chown=worklens:worklens \
    /workspace/target/worklens_backend-0.0.1-SNAPSHOT.jar \
    /app/worklens-backend.jar

USER worklens

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/worklens-backend.jar"]
