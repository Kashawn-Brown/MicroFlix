# --- Build stage: compile the app ---

# Start a new image based on maven:3.9-eclipse-temurin-21 (has Linux, Maven 3.9, Java21 (Temurin/OpenJDK))
# Call it 'build' to reference it later (naming image)
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the Maven parent POM and modules to build just the gateway module
COPY pom.xml .
COPY modules ./modules

# Move into the gateway module then build it
WORKDIR /app/modules/gateway
RUN mvn clean package -DskipTests



# --- Runtime stage: small image just to run the JAR ---

# Start a new, smaller image that just has: Alpine Linux (small OS), Java 21 runtime (JRE -> to run JARs; no compiler)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built gateway jar from the build stage
COPY --from=build /app/modules/gateway/target/gateway-*.jar app.jar

# Expose the gateway port
EXPOSE 8081

# Allow passing extra JVM options via JAVA_OPTS (e.g., memory settings)
ENV JAVA_OPTS=""

# Start the gateway application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]