# ---- Stage 1: Build the application ----
FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ---- Stage 2: Run the application ----
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/shopping-cart-1.0.0.jar app.jar

# Set environment variable for dynamic port (Railway sets PORT at runtime)
ENV PORT=8080

# Run the app — Spring Boot will pick up the PORT from environment variables
ENTRYPOINT ["java", "-jar", "app.jar"]
