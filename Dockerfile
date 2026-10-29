# ---------- Giai đoạn 1: Build ----------
# Dùng image có sẵn JDK 21 + Gradle để build, không phụ thuộc máy bạn.
FROM gradle:8.11-jdk21 AS build
WORKDIR /app

# Copy file cấu hình build trước để tận dụng cache của Docker
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy toàn bộ mã nguồn
COPY src ./src

# Build ra file .jar (bỏ qua test để build nhanh; muốn chạy test thì bỏ -x test)
RUN gradle bootJar --no-daemon -x test

# ---------- Giai đoạn 2: Run ----------
# Image gọn nhẹ chỉ chứa JRE để chạy app
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy file jar đã build từ giai đoạn trên
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
