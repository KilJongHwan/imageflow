FROM gradle:8.10.2-jdk17 AS builder

WORKDIR /workspace
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

# 512MB 인스턴스에서 컨테이너 OOM(인스턴스 통째 재시작)을 막으려면, 힙만 제한할 게 아니라
# 힙 + 비힙(metaspace, 코드 캐시, 스레드 스택) + off-heap(direct memory)의 "합계"를 천장 밑으로 눌러야 한다.
# 각 영역 상한을 명시해 총합을 ~460MB로 묶고 ~50MB 헤드룸을 남긴다:
#   - Xmx 192m        : 힙. 배치 업로드(합계 40MB) + Spring 기저(~100MB)를 담고도 여유가 있는 선.
#   - MaxMetaspaceSize: 클래스 메타데이터가 무한정 커지지 않게 상한.
#   - ReservedCodeCache: JIT 코드 캐시 상한(기본 240MB 예약분을 줄임).
#   - MaxDirectMemorySize: Netty/Lettuce(Redis)가 쓰는 off-heap 버퍼 상한. 기본값이 Xmx와 같아
#                          힙 밖에서 RSS를 조용히 밀어올리던 숨은 구멍을 막는다.
#   - Xss512k         : 스레드 스택 절반으로.
#   - ExitOnOutOfMemoryError : 힙이 정말 꽉 차면 좀비로 버티지 말고 즉시 깨끗하게 재시작.
# Serial GC는 조각 CPU + 작은 힙에서 GC 오버헤드와 네이티브 메모리를 가장 낮게 유지한다.
ENTRYPOINT ["java", \
  "-Xms64m", \
  "-Xmx192m", \
  "-XX:+UseSerialGC", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:ReservedCodeCacheSize=48m", \
  "-XX:MaxDirectMemorySize=48m", \
  "-Xss512k", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "/app/app.jar"]
