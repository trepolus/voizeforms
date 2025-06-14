FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

# Copy project files into container and ensure wrapper is executable
COPY . .
RUN chmod +x gradlew

# Set environment variables to avoid using SVE instructions for ARM64 M4 Processor
ENV JAVA_OPTS="-XX:UseSVE=0"
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

# Build app
RUN ./gradlew clean build -x test --no-daemon

# List contents of build/libs to see what JAR was created
RUN ls -la build/libs/

# Copy JAR file into the container and run app
RUN cp build/libs/voizeforms-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
