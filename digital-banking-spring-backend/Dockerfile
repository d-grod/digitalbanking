# Build stage
FROM maven:3.8.1-openjdk-17

WORKDIR /app
 
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
 
COPY src ./src

EXPOSE 8085

CMD ["./mvnw", "spring-boot:run"]