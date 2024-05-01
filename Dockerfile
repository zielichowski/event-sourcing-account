FROM eclipse-temurin:21.0.2_13-jre
VOLUME /tmp
EXPOSE 8080
ENV SERVICE_NAME=event-sourcing-account

COPY build/libs/*.jar event-sourcing-account.jar
ENTRYPOINT ["java","-jar", "/event-sourcing-account.jar"]