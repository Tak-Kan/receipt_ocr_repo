FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
# Spring Boot の実行可能 JAR を作成
RUN mvn package -DskipTests spring-boot:repackage

FROM eclipse-temurin:17-jre
WORKDIR /app
# jar 名を明示
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
