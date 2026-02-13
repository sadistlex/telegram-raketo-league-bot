FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    apk del maven

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Memory-optimized JVM settings for a small bot application
# -Xms: Initial heap size (128MB)
# -Xmx: Maximum heap size (256MB - adjust based on actual usage)
# -XX:+UseG1GC: Use G1 garbage collector (good for containers)
# -XX:MaxGCPauseMillis: Target max GC pause
# -XX:+UseStringDeduplication: Reduce memory for duplicate strings
# -XX:+UseContainerSupport: Respect container memory limits
# -XX:MaxRAMPercentage: Use max 75% of container RAM if -Xmx not set
ENTRYPOINT ["java", \
    "-Xms128m", \
    "-Xmx256m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-XX:+UseStringDeduplication", \
    "-XX:+UseContainerSupport", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

