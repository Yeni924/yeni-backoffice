FROM eclipse-temurin:17-jdk AS build

WORKDIR /app
COPY . .

RUN chmod +x ./gradlew && ./gradlew clean :api:bootJar -x test

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /app/api/build/libs/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-Xmx384m -XX:+UseSerialGC"
ENV SPRING_PROFILES_ACTIVE="fly"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
