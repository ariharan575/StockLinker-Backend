# ==========================
# Stage 1 - Build
# ==========================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and pom
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw

# Download dependencies (cached)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# ==========================
# Stage 2 - Runtime
# ==========================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user
RUN useradd -ms /bin/bash spring

# Copy jar from builder
COPY --from=builder /app/target/*.jar app.jar

RUN chown -R spring:spring /app

USER spring

# Remove PORT=8080 and let Render handle it
# ENV PORT=8080  <-- REMOVE THIS LINE

EXPOSE 8080

ENTRYPOINT ["sh","-c","java \
-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75.0 \
-Djava.security.egd=file:/dev/./urandom \
-jar app.jar"]