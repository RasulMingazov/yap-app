# syntax=docker/dockerfile:1

# Builds and runs the Ktor server (services/server/app). The mobile modules stay in the Gradle
# graph, so the build image only needs a JDK: nothing Android-specific is compiled here.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --no-configuration-cache \
      -Dorg.gradle.jvmargs="-Xmx2g -Dfile.encoding=UTF-8" \
      :services:server:app:installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/server/app/build/install/app /app
ENV PORT=8080
EXPOSE 8080
CMD ["/app/bin/app"]
