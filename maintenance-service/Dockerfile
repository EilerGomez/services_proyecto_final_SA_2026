FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.source=https://github.com/EilerGomez/services_proyecto_final_SA_2026
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
# El JAR se construye antes con: mvn -pl maintenance-service -am package
COPY target/maintenance-service.jar app.jar
USER app
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
