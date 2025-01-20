FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar
EXPOSE 8080
CMD ["java", "-jar", "build/libs/*.jar"]