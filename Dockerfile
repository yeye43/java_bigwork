FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir -p target/classes \
    && javac -encoding UTF-8 -d target/classes $(find src/main/java -name "*.java")

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes ./target/classes
RUN mkdir -p records
ENV PORT=8080
EXPOSE 8080
CMD ["sh", "-c", "java -cp target/classes edu.jieqi.web.JieqiWebServer ${PORT}"]
