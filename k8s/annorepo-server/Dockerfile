FROM openjdk:18.0.1.1-slim-buster

WORKDIR /apps/annorepo
COPY ./target/annorepo-*.jar /apps/annorepo/annorepo.jar
COPY ./k8s/annorepo-server/entrypoint.sh entrypoint.sh
COPY ./config.yml config.yml
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]