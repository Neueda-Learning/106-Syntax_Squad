# ── Backend Build ─────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS backend-builder
WORKDIR /app/backend
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# ── Backend Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS backend
WORKDIR /app
RUN addgroup -S tmsgroup && adduser -S tmsuser -G tmsgroup
COPY --from=backend-builder /app/backend/target/payment-backend-*.jar app.jar
RUN chown tmsuser:tmsgroup app.jar
USER tmsuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# ── Frontend Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --silent
COPY frontend/ .
RUN npm run build

# ── Frontend Runtime ──────────────────────────────────────────────────────────
FROM nginx:alpine AS frontend
COPY --from=frontend-builder /app/frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:3000/health || exit 1
CMD ["nginx", "-g", "daemon off;"]