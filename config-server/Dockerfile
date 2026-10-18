FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY config-server/pom.xml config-server/pom.xml
RUN mvn -B -f config-server/pom.xml dependency:go-offline
COPY config-server/src/ config-server/src/
RUN mvn -B -f config-server/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/config-server/target/config-server-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
