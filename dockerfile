# Use Maven with JDK 21 image
FROM maven:3.9.4-eclipse-temurin-21

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package -DskipTests

# Expose port (adjust if your app uses a different port)
EXPOSE 8070

# Run the packaged jar (adjust jar name if different)
CMD ["java", "-jar", "target/shopping-cart-1.0.jar"]
