# frontend/Dockerfile (multi-stage build)
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 依存解決のため先に pom をコピーして依存をダウンロード
COPY pom.xml .
# キャッシュ用に依存解決のみ
RUN mvn -B -f pom.xml dependency:go-offline -DskipTests
# RUN --mount=type=cache,target=/root/.m2 mvn -B -f pom.xml dependency:go-offline -DskipTests

# ソースをコピーしてビルド
COPY src ./src
RUN mvn -B -f pom.xml package -DskipTests
# RUN --mount=type=cache,target=/root/.m2 mvn -B -f pom.xml package -DskipTests

# ランタイムイメージ
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY rds-ca.pem /app/rds-ca.pem
# 証明書をJavaのTrustStoreへ登録
RUN keytool -importcert \
    -noprompt \
    -trustcacerts \
    -alias rds-ca \
    -file /app/rds-ca.pem \
    -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit
COPY --from=builder /build/target/demo-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]