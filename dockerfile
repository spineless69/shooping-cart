FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

EXPOSE 8070

CMD ["java", "-jar", "target/shopping-cart-1.0.jar"]
