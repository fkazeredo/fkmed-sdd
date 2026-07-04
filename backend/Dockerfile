# FKMed backend — multi-stage build (BOOTSTRAP §3).
# Build context is the REPOSITORY ROOT (compose/CI set `context: .`), so git metadata is
# available to the git-commit-id plugin feeding /api/system/version.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY backend/mvnw backend/mvnw
COPY backend/.mvn backend/.mvn
COPY backend/pom.xml backend/pom.xml
RUN chmod +x backend/mvnw && cd backend && ./mvnw -B -q dependency:go-offline || true
COPY backend backend
COPY .git .git
RUN cd backend && ./mvnw -B -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 fkmed
USER fkmed
WORKDIR /app
COPY --from=build /workspace/backend/target/fkmed-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
