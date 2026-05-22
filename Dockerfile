FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-oracle
WORKDIR /app
COPY --from=build /app/target/aesty-messenger-0.0.1-SNAPSHOT.jar /app/messenger.jar
ENTRYPOINT ["java", "-jar", "messenger.jar"]