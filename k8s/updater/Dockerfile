FROM alpine:3.14
RUN apk add --no-cache bash curl && \
    groupadd -r annorepo && \
    useradd --no-log-init -r -g annorepo annorepo
USER annorepo
WORKDIR /work
COPY ./k8s/updater/entrypoint.sh entrypoint.sh
COPY ./k8s/updater/wait-for-it.sh wait-for-it.sh
ENTRYPOINT ["./entrypoint.sh"]