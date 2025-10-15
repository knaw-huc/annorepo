FROM openjdk:25-rc-jdk-slim-trixie

RUN microdnf update && \
    microdnf install -y curl && \
    groupadd -r annorepo && \
    useradd --no-log-init -r -g annorepo annorepo
USER annorepo
WORKDIR /apps/annorepo
COPY ./server/target/annorepo-server-*.jar /apps/annorepo/annorepo-server.jar
COPY ./k8s/annorepo-server/entrypoint.sh entrypoint.sh
COPY config.yml config.yml
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]