FROM eclipse-temurin:21.0.2_13-jre
VOLUME /tmp
EXPOSE 8080
ENV SERVICE_NAME=event-sourcing-account

COPY account-service/build/libs/account-service-1.0.0.jar account-service.jar
ENTRYPOINT ["java","-jar", "/account-service.jar"]