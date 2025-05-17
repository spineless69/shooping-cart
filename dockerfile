# Stage 1: Build using Maven with Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run with lightweight Java 21 runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/shopping-cart-1.0.jar app.jar
EXPOSE 8070
CMD ["java", "-jar", "app.jar"]
