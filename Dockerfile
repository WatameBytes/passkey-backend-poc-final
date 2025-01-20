FROM eclipse-temurin:17-jdk-jammy

# Set the working directory inside the container
WORKDIR /app

# Copy only necessary files to the container
COPY gradlew gradlew
COPY gradle gradle
COPY src src
COPY build.gradle settings.gradle ./

# Give executable permissions to the Gradle wrapper
RUN chmod +x ./gradlew

# Build the Spring Boot JAR file
RUN ./gradlew bootJar

# Expose the application port
EXPOSE 8080

# The JAR file path should match your build output
CMD ["java", "-jar", "build/libs/Backend-Passkey-Final-1.0-SNAPSHOT.jar"]