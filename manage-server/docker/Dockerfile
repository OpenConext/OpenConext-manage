FROM eclipse-temurin:21-jdk-alpine
COPY target/*.jar app.jar

HEALTHCHECK --interval=10s --timeout=10s --start-period=10s --retries=3 \
  CMD ["wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/internal/health"]

ENTRYPOINT ["java","-jar","/app.jar"]
