# ============================================================
# build stage — 이미지 빌드는 GitHub Actions에서 돈다.
# 서버(t3.micro, 램 1GB)에서 직접 빌드하면 다른 컨테이너가 OOM으로 죽는다.
# ============================================================
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# 의존성 레이어를 소스보다 먼저 만들어 캐시를 살린다.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew clean bootJar --no-daemon -x test

# ============================================================
# runtime stage — JDK가 아니라 JRE. 이미지가 절반 이하로 줄어든다.
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# root로 돌리지 않는다.
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

# 서버 램이 1GB뿐이라 힙을 명시적으로 묶는다.
# 컨테이너 실사용은 힙 + 메타스페이스 + 스레드 스택이라 384m보다 늘 크다.
ENV JAVA_OPTS="-Xms256m -Xmx384m -XX:MaxMetaspaceSize=128m"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
