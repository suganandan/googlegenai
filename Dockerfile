#Start with a base image containing Java runtime
FROM openjdk:17-jdk-slim

#Information who maintains the image
MAINTAINER suga.com


# Use official OpenJDK image as a base image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the packaged Spring Boot application JAR file into the container
COPY target/googlegenai-0.0.1-SNAPSHOT.jar googlegenai.jar

# Expose the port that the Spring Boot application will run on
EXPOSE 8080

# Run the Spring Boot application when the container starts
CMD ["java", "-jar", "googlegenai.jar"]
