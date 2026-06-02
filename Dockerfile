FROM eclipse-temurin:17-jdk-focal AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle .
COPY src src
RUN chmod +x gradlew
RUN ./gradlew bootJar -x test
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
