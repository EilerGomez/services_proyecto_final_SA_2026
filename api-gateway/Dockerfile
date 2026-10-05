FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.source=https://github.com/EilerGomez/services_proyecto_final_SA_2026
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
# El JAR se construye antes con: mvn -pl api-gateway -am package
COPY target/api-gateway.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
