# --- Build stage: compile the app ---

# Start a new image based on maven:3.9-eclipse-temurin-21 (has Linux, Maven 3.9, Java21 (Temurin/OpenJDK))
# Call it 'build' to reference it later (naming image)
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent pom + modules
COPY pom.xml .
COPY modules ./modules

WORKDIR /app/modules/discovery
RUN mvn clean package -DskipTests



# --- Runtime stage: small image just to run the JAR ---

# Start a new, smaller image that just has: Alpine Linux (small OS), Java 21 runtime (JRE -> to run JARs; no compiler)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built discovery jar from the build stage
COPY --from=build /app/modules/discovery/target/discovery-*.jar app.jar

# Expose the port
EXPOSE 8761

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]