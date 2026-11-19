FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY starship-contract/ starship-contract/
RUN mvn -B -f starship-contract/pom.xml -DskipTests install
COPY starship-rest-adapter/ starship-rest-adapter/
RUN mvn -B -f starship-rest-adapter/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/starship-rest-adapter/target/starship-rest-adapter-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
