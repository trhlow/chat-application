FROM maven:3.9.9-eclipse-temurin-21 AS jvm-builder
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
RUN mvn -q -DskipTests dependency:go-offline

COPY src src
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre AS jvm
WORKDIR /app
COPY --from=jvm-builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM ghcr.io/graalvm/native-image-community:21 AS native-builder
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY src src
RUN chmod +x mvnw
RUN ./mvnw -q -Pnative -DskipTests native:compile

FROM gcr.io/distroless/base-debian12 AS native
WORKDIR /app
COPY --from=native-builder /app/target/in-chat app
EXPOSE 8080
ENTRYPOINT ["/app/app"]
