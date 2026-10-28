# Build web
FROM node:20-alpine AS web
WORKDIR /web
COPY web/package*.json ./
RUN npm install
COPY web/ ./
RUN npm run build

# Build backend with the SPA inside
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY backend/ ./
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY deploy/certs/russian_trusted_root_ca.crt /tmp/russian_trusted_root_ca.crt
COPY deploy/certs/russian_trusted_sub_ca.crt /tmp/russian_trusted_sub_ca.crt
RUN keytool -importcert -noprompt -alias russian_trusted_root \
        -file /tmp/russian_trusted_root_ca.crt \
        -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit \
    && keytool -importcert -noprompt -alias russian_trusted_sub \
        -file /tmp/russian_trusted_sub_ca.crt \
        -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit
COPY --from=backend /app/target/antenatal-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
