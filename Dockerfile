FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml files first to take advantage of Docker layer caching
COPY pom.xml ./
COPY feature-flag-service/pom.xml ./feature-flag-service/
COPY feature-flag-client/pom.xml ./feature-flag-client/

# Download all required dependencies into one layer
RUN mvn dependency:go-offline -B

# Copy source code
COPY feature-flag-service/src ./feature-flag-service/src
COPY feature-flag-client/src ./feature-flag-client/src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built artifact from the build stage
COPY --from=build /app/feature-flag-service/target/*.jar app.jar

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

