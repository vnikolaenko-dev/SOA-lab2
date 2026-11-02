FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY eureka-server/pom.xml eureka-server/pom.xml
RUN mvn -B -f eureka-server/pom.xml dependency:go-offline
COPY eureka-server/src/ eureka-server/src/
RUN mvn -B -f eureka-server/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/eureka-server/target/eureka-server-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
