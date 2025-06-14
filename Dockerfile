FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy project files into container and ensure wrapper is executable
COPY . .
RUN chmod +x gradlew

# Set environment variables
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

# Build app
RUN ./gradlew clean build -x test --no-daemon

# List contents of build/libs to see what JAR was created
RUN ls -la build/libs/

# Copy JAR file into the container and run app
RUN cp build/libs/voizeforms-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
