FROM eclipse-temurin:21.0.2_13-jre
VOLUME /tmp
EXPOSE 8081
ENV SERVICE_NAME=loyalty-service

COPY loyalty-service/build/libs/loyalty-service-1.0.0.jar loyalty-service.jar
ENTRYPOINT ["java","-jar", "/loyalty-service.jar"]