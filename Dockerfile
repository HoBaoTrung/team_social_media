# # Stage 1: Build JAR with Gradle
# FROM gradle:8.5-jdk17 AS builder
# WORKDIR /app

# # Copy Gradle wrapper & config trước (để tối ưu cache)
# COPY social-media/gradlew ./gradlew
# COPY social-media/gradle ./gradle
# COPY social-media/build.gradle social-media/settings.gradle ./

# # Cấp quyền thực thi cho gradlew (fix lỗi Permission denied trên Linux)
# RUN chmod +x ./gradlew

# # Copy toàn bộ source code vào container
# COPY social-media ./ 

# # Build ứng dụng, bỏ qua test để nhanh hơn
# RUN ./gradlew clean build -x test --no-daemon

# # Stage 2: Run application
# FROM eclipse-temurin:17-jre-alpine
# WORKDIR /app

# # Copy file JAR từ stage build sang stage run
# COPY --from=builder /app/build/libs/*.jar app.jar

# # Chạy ứng dụng
# ENTRYPOINT ["java","-jar","app.jar"]
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
