# --- Build stage: compile the app ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent pom + modules into the image
COPY pom.xml ./pom.xml
COPY modules ./modules

# Build ONLY the tmdb-ingestion-service module
WORKDIR /app/modules/tmdb-ingestion-service
RUN mvn clean package -DskipTests

# --- Run stage: lightweight JRE image ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/modules/tmdb-ingestion-service/target/tmdb-ingestion-service-*.jar app.jar

# Optional JVM opts (heap size, etc.)
ENV JAVA_OPTS=""

# This runs the ingestion job when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
