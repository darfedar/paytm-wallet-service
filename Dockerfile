FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S wallet && adduser -S wallet -G wallet
WORKDIR /app
COPY --from=build /workspace/target/wallet-service-1.0.0.jar /app/app.jar
RUN chown -R wallet:wallet /app
USER wallet
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=10 CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
