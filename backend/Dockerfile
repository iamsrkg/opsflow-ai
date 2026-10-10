# Build with Maven, run on a JRE: the final image has no compiler and no source.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home opsflow
COPY --from=build /app/target/opsflow-backend-*.jar app.jar
USER opsflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
