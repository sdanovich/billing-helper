# Runtime image for the claims backend.
#
# The Spring Boot fat jar bundles everything — including the platform-stack auth classes
# pulled from the local Maven repo — so this image just runs it. Build the jar first from
# the repo root (it needs the JDK 21 + mavenLocal toolchain that aren't in this image):
#
#   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -pl claims-backend -am -DskipTests package
#   docker compose build backend
#
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/claims-backend.jar app.jar
EXPOSE 28587
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
