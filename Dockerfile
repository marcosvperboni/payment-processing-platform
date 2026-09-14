FROM docker.io/library/maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q clean package -DskipTests

FROM docker.io/library/eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/payment-processing-platform-*.jar app.jar
EXPOSE 18095
ENTRYPOINT ["java", "-jar", "app.jar"]
