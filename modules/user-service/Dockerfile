# --- Build stage: compile the app ---

# Start a new image based on maven:3.9-eclipse-temurin-21 (has Linux, Maven 3.9, Java21 (Temurin/OpenJDK))
# Call it 'build' to reference it later (naming image)
FROM maven:3.9-eclipse-temurin-21 AS build

# Inside the container, “cd” into /app
WORKDIR /app

# From the build context, copy pom.xml into /app/pom.xml (Copy the parent pom (multi-module definition))
COPY pom.xml ./pom.xml

# Copy the entire modules folder from your repo into /app/modules in the container (including user-service source)
COPY modules ./modules

# Move into the user-service module then build it
WORKDIR /app/modules/user-service
RUN mvn clean package -DskipTests

# Result: Maven compiles user-service and produces a JAR at: /app/modules/user-service/target/<something>.jar

# --- Runtime stage: small image just to run the JAR ---

# Start a new, smaller image that just has: Alpine Linux (small OS), Java 21 runtime (JRE -> to run JARs; no compiler)
FROM eclipse-temurin:21-jre-alpine

# cd into app (DIFFERENT -> separate image with separate filesystem (Every time Docker hits a FROM in a Dockerfile))
WORKDIR /app

# Copy the built jar from the build stage
# Take the file from the build stage’s filesystem and copy it into the runtime stage’s filesystem
COPY --from=build /app/modules/user-service/target/*.jar app.jar

# Optional: JVM options (memory, etc.) can be injected via JAVA_OPTS
ENV JAVA_OPTS=""

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
