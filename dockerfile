FROM eclipse-temurin:21-jdk-alpine
FROM maven:3.9.9-eclipse-temurin-17-alpine
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
EXPOSE 8070
CMD ["java", "-jar", "target/shopping-cart-1.0.jar"]


