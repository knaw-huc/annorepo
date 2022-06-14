FROM maven:3.8.5 as builder

WORKDIR /build/
COPY ./src /build/src
COPY ./pom.xml /build/
RUN mvn --no-transfer-progress --batch-mode --update-snapshots package

FROM openjdk:18.0.1.1-slim-buster

WORKDIR /apps/annorepo
COPY --from=builder /build/target/annorepo-*.jar /apps/annorepo/annorepo.jar
COPY ./k8s/annorepo-server/entrypoint.sh entrypoint.sh
COPY ./config.yml config.yml
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]
