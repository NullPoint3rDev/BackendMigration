# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /build/target/WT2Curs-1.0-SNAPSHOT.jar app.jar
EXPOSE 8085 3003
ENV SPRING_PROFILES_ACTIVE=staging
ENTRYPOINT ["java", "-jar", "app.jar"]
