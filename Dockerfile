#Start with a base image containing Java runtime
FROM openjdk:17-jdk-slim

#Information who maintains the image
MAINTAINER suga.com

COPY target/googlegenai-0.0.1-SNAPSHOT.jar googlegenai-0.0.1-SNAPSHOT

ENTRYPOINT ["java","-jar","googlegenai-0.0.1-SNAPSHOT"]