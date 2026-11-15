
# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy Event Contracts
COPY event-contracts event-contracts

# Copy Payment Service
COPY payment-service payment-service

# Install Event Contracts into Maven local repository
RUN mvn -q -f event-contracts/pom.xml -DskipTests install

# Build Payment Service
RUN mvn -q -f payment-service/pom.xml -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user
RUN useradd --system --uid 10001 appuser

# Copy executable JAR
COPY --from=build /workspace/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar app.jar

USER 10001

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
