# Use Eclipse Temurin JRE 21 (OpenJDK)
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the packaged jar file
COPY target/password-strength-checker-cli-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar", "--interactive"]
