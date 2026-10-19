FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY event-contracts event-contracts
COPY order-service order-service
RUN mvn -q -f event-contracts/pom.xml -DskipTests install && mvn -q -f order-service/pom.xml -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/order-service/target/order-service-0.0.1-SNAPSHOT.jar app.jar
USER 10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
