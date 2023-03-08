FROM openjdk:20-rc-jdk-slim

RUN apt update && \
    apt install -y curl && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /apps/annorepo
COPY ./server/target/annorepo-server-*.jar /apps/annorepo/annorepo-server.jar
COPY ./k8s/annorepo-server/entrypoint.sh entrypoint.sh
COPY ./config.yml config.yml
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]