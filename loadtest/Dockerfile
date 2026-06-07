FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw && ./mvnw -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/loadtest-*.jar app.jar

EXPOSE 4200
ENTRYPOINT ["java", "-jar", "app.jar"]
