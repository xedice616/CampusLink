# Build context: backend/ (also used by render.yaml and compose.yaml).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 campuslink && useradd --uid 10001 --gid campuslink --no-create-home campuslink \
    && chown campuslink:campuslink /app
COPY --from=build --chown=campuslink:campuslink /build/target/campuslink-backend-1.0.0.jar /app/app.jar
USER campuslink
ENV PORT=8787
EXPOSE 8787
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
