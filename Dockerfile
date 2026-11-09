# Docker multi-stage build

# 1. Building the App with Maven
FROM maven:3.9-eclipse-temurin-25 AS build

ADD . /MediterraneApp
WORKDIR /MediterraneApp

# Just echo so we can see, if everything is there :)
RUN ls -l

# Run Maven build
RUN mvn clean install


# 2. Just using the build artifact and then removing the build-container
FROM eclipse-temurin:25-jre

MAINTAINER El_Giancar & Lleda

VOLUME /tmp

# Add Spring Boot app.jar to Container
COPY --from=build "MediterraneApp/target/*.jar" app.jar

# Fire up our Spring Boot app by default
CMD [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
