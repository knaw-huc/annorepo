FROM openjdk:18.0.1.1-slim-buster

RUN apt update && apt install curl
WORKDIR /apps/annorepo
COPY ./server/target/annorepo-server-*.jar /apps/annorepo/annorepo-server.jar
COPY ./k8s/annorepo-server/entrypoint.sh entrypoint.sh
COPY ./config.yml config.yml
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]