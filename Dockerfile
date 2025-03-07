FROM amazoncorretto:23 AS runtime
EXPOSE 8080
RUN mkdir /app
COPY ./build/libs/KalaBPMNv3-all.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]