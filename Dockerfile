FROM node:22-bookworm-slim AS web
WORKDIR /web
ARG VITE_SHOW_DEMO_ACCOUNT=false
ENV VITE_SHOW_DEMO_ACCOUNT=$VITE_SHOW_DEMO_ACCOUNT
RUN npm install --global pnpm@10.17.1
COPY web/package.json web/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY web/ ./
RUN pnpm build

FROM eclipse-temurin:25-jdk AS server
WORKDIR /server
COPY server/gradlew server/settings.gradle.kts server/build.gradle.kts ./
COPY server/gradle ./gradle
RUN chmod +x gradlew
COPY server/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=server --chown=10001:10001 /server/build/libs/*.jar app.jar
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
