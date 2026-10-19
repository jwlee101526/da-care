FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew
COPY src src
# Gradle 배포판과 의존성 jar를 빌드 캐시에 두어, 소스만 바뀐 빌드에서 다시 내려받지 않는다.
RUN --mount=type=cache,id=gradle,target=/root/.gradle ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
