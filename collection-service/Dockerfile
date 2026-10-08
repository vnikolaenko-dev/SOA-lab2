FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY collection-service/pom.xml collection-service/pom.xml
RUN mvn -B -f collection-service/pom.xml dependency:go-offline

COPY openapi-templates/ openapi-templates/
COPY collection-service/src/ collection-service/src/
RUN mvn -B -f collection-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/collection-service/target/collection-service-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
