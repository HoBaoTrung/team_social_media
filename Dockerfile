
# Stage 1: Build JAR with Gradle
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

COPY social-media/ ./
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Run application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
