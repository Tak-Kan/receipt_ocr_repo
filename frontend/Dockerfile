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
COPY ap-northeast-3-bundle.pem /app/ap-northeast-3-bundle.pem
# RDSのCA証明書をJavaのTrustStoreへ登録する。
# バンドルには複数の証明書（ルートCA）が入っているが、keytool -importcert は1つのファイルから
# 先頭の1枚しか登録しない。そのため、1枚ずつのファイルに分割して、すべてを別名で登録する。
# （RDSがどのルートCAで署名されていても、sslMode=verify-ca の検証が通るようにするため）
RUN set -eu; \
    mkdir -p /tmp/rds-ca; \
    awk 'split_after == 1 { n++; split_after = 0 } \
         /-----END CERTIFICATE-----/ { split_after = 1 } \
         { print > ("/tmp/rds-ca/cert-" (n + 1) ".pem") }' /app/ap-northeast-3-bundle.pem; \
    count=0; \
    for cert in /tmp/rds-ca/cert-*.pem; do \
        grep -q 'BEGIN CERTIFICATE' "$cert" || continue; \
        count=$((count + 1)); \
        keytool -importcert -noprompt -trustcacerts \
            -alias "ap-northeast-3-bundle-${count}" \
            -file "$cert" \
            -keystore "$JAVA_HOME/lib/security/cacerts" \
            -storepass changeit; \
    done; \
    [ "$count" -gt 0 ] || { echo "ERROR: no certificate found in ap-northeast-3-bundle.pem" >&2; exit 1; }; \
    rm -rf /tmp/rds-ca
COPY --from=builder /build/target/demo-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]