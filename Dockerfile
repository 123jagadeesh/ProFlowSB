# ==========================================
# Stage 1: Build the application with Maven
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml and pre-fetch dependencies for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build jar without running tests during image build
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Lightweight runtime image
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create uploads directory and assign ownership
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Copy executable jar from builder stage
COPY --from=builder /app/target/proflow-server-*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

# Switch to non-root user
USER spring:spring

# Expose default port (Render automatically assigns $PORT at runtime)
EXPOSE 5000

# Optimize JVM flags for container / low memory environments like Render free tier (512MB RAM)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "/app/app.jar"]
