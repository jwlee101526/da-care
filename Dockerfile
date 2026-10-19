FROM node:22-bookworm-slim AS web
WORKDIR /web
ARG VITE_SHOW_DEMO_ACCOUNT=false
ENV VITE_SHOW_DEMO_ACCOUNT=$VITE_SHOW_DEMO_ACCOUNT
RUN npm install --global pnpm@10.17.1
COPY web/package.json web/pnpm-lock.yaml ./
# pnpm 패키지 저장소를 빌드 캐시에 두어, 잠금 파일이 바뀌어도 이미 받은 패키지는 다시 내려받지 않는다.
RUN --mount=type=cache,id=pnpm-store,target=/pnpm/store pnpm install --frozen-lockfile --store-dir /pnpm/store
COPY web/ ./
RUN pnpm build

FROM eclipse-temurin:25-jdk AS server
WORKDIR /server
COPY server/gradlew server/settings.gradle.kts server/build.gradle.kts ./
COPY server/gradle ./gradle
RUN chmod +x gradlew
COPY server/src ./src
COPY --from=web /web/dist ./src/main/resources/static
# Gradle 배포판과 의존성 jar를 빌드 캐시에 두어, 소스만 바뀐 빌드에서 다시 내려받지 않는다.
RUN --mount=type=cache,id=gradle,target=/root/.gradle ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=server --chown=10001:10001 /server/build/libs/*.jar app.jar
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
