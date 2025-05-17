# Use Maven image with JDK 21
FROM maven:3.9.4-eclipse-temurin-21

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the project (skip tests)
RUN mvn clean package -DskipTests

# Expose port your app listens on
EXPOSE 8080

# Run the jar file (adjust jar name as needed)
CMD ["java", "-jar", "target/shopping-cart-1.0.jar"]
