FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY starship-service/pom.xml starship-service/pom.xml
RUN mvn -B -f starship-service/pom.xml dependency:go-offline

COPY openapi-templates/ openapi-templates/
COPY starship-service/src/ starship-service/src/
RUN mvn -B -f starship-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/starship-service/target/starship-service-1.0.0.war app.war
ENTRYPOINT ["java", "-jar", "/app/app.war"]
