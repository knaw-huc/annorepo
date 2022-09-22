FROM alpine:3.14
RUN apk add --no-cache bash curl
WORKDIR /work
COPY ./k8s/updater/entrypoint.sh entrypoint.sh
COPY ./k8s/updater/wait-for-it.sh wait-for-it.sh
ENTRYPOINT ["./entrypoint.sh"]