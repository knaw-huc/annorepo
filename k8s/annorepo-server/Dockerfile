FROM amazoncorretto:25-alpine

RUN apk add --no-cache curl && \
    addgroup -S annorepo && \
    adduser -S -G annorepo -H -D annorepo

WORKDIR /app/annorepo
COPY --chown=annorepo:annorepo ./server/target/annorepo-server-*.jar annorepo-server.jar
COPY --chown=annorepo:annorepo --chmod=755 ./k8s/annorepo-server/entrypoint.sh entrypoint.sh
COPY --chown=annorepo:annorepo ./k8s/annorepo-server/config.yml config.yml

USER annorepo
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]
