# ---- Stage 1: Build the application ----
FROM maven:3.9.4-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ---- Stage 2: Run the application ----
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the built JAR from the first stage
COPY --from=build /app/target/shopping-cart-1.0.0.jar app.jar

# Expose the application port
EXPOSE 8070

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
