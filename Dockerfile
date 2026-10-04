# --- Stage 1: Build ---
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Download dependencies first (cache layer with BuildKit mount)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -q

# Build application
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -q


# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app


# Install curl and certificates
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        ca-certificates \
        openssl && \
    rm -rf /var/lib/apt/lists/*


# Download AWS RDS / DocumentDB CA bundle and import into JVM truststore
RUN curl -fsSL \
    https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem \
    -o /tmp/global-bundle.pem && \
    csplit -z -f /tmp/aws-cert- /tmp/global-bundle.pem \
        '/-----BEGIN CERTIFICATE-----/' '{*}' && \
    for cert in /tmp/aws-cert-*; do \
        keytool \
            -importcert \
            -trustcacerts \
            -alias $(basename $cert) \
            -file $cert \
            -cacerts \
            -storepass changeit \
            -noprompt || true; \
    done && \
    rm -rf /tmp/aws-cert-* /tmp/global-bundle.pem


# Create non-root user
RUN groupadd -r officyna && \
    useradd -r -g officyna -d /app officyna

# New Relic Configuration with proper ownership
RUN mkdir -p /usr/local/newrelic && chown -R officyna:officyna /usr/local/newrelic
COPY --chown=officyna:officyna ./newrelic/newrelic.jar /usr/local/newrelic/newrelic.jar
COPY --chown=officyna:officyna ./newrelic/newrelic.yml /usr/local/newrelic/newrelic.yml

# Direct New Relic logs to STDOUT in container environment
ENV NEW_RELIC_LOG=STDOUT

USER officyna


COPY --chown=officyna:officyna --from=builder /app/target/officyna-os-service-*.jar app.jar


EXPOSE 8080


HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1


ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-javaagent:/usr/local/newrelic/newrelic.jar", \
    "-Djavax.net.ssl.trustStorePassword=changeit", \
    "-jar", "app.jar"]