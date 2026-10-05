FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.source=https://github.com/EilerGomez/services_proyecto_final_SA_2026
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
# El JAR se construye antes con: mvn -pl notification-service -am package
COPY target/notification-service.jar app.jar
USER app
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
