FROM gradle:8.10.2-jdk17 AS builder

WORKDIR /workspace
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

# 512MB 인스턴스는 비힙(metaspace, 코드 캐시, 스레드 스택)으로도 ~150MB를 써야 하므로,
# 커널 OOM kill을 피하려고 힙을 50%로 제한한다: 자바 힙 OutOfMemoryError는 요청 하나만 죽지만
# 컨테이너 한도를 넘으면 인스턴스가 통째로 재시작된다.
# Serial GC는 조각 CPU에 작은 힙 환경에서 GC 오버헤드를 낮게 유지한다.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
