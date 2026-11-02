FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn -B -f api-gateway/pom.xml dependency:go-offline
COPY api-gateway/src/ api-gateway/src/
RUN mvn -B -f api-gateway/pom.xml -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /build/api-gateway/target/api-gateway-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
