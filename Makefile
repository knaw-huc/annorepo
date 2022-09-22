all: help
TAG = annorepo
DOCKER_DOMAIN = registry.diginfra.net/tt

.make:
	mkdir -p .make

.make/.version: .make pom.xml
	mvn help:evaluate -Dexpression=project.version -q -DforceStdout > .make/.version

server/target/annorepo-server-$(shell cat .make/.version).jar: .make/.version  $(shell find server/src common/src -type f) pom.xml server/pom.xml
	mvn --projects server --also-make package

client/target/annorepo-client-$(shell cat .make/.version).jar: .make/.version  $(shell find client/src common/src -type f) pom.xml client/pom.xml
	mvn --projects client --also-make package

.PHONY: build
build: .make/.version server/target/annorepo-server-$(shell cat .make/.version).jar client/target/annorepo-client-$(shell cat .make/.version).jar

.PHONY: build-server
build-server: .make/.version server/target/annorepo-server-$(shell cat .make/.version).jar

.PHONY: build-client
build-client: .make/.version client/target/annorepo-client-$(shell cat .make/.version).jar

.PHONY: run-server
run-server: build-server
	java -jar server/target/annorepo-server-$(shell cat .make/.version).jar server config.yml

.PHONY: run-env
run-env: build-server
	java -jar server/target/annorepo-server-$(shell cat .make/.version).jar env

.PHONY: docker-run
docker-run: k8s/local/docker-compose.yml
	cd k8s/local && docker compose up &

.PHONY: docker-stop
docker-stop: k8s/local/docker-compose.yml
	cd k8s/local && docker compose down

.make/.docker: .make k8s/annorepo-server/Dockerfile-multistage
	docker build -t $(TAG):$(shell cat .make/.version) -f k8s/annorepo-server/Dockerfile-multistage .
	@touch $@

.PHONY: docker-image
docker-image: .make/.docker

.make/.push-server: build k8s/annorepo-server/Dockerfile
	docker build -t $(TAG):$(shell cat .make/.version) --platform=linux/amd64 -f k8s/annorepo-server/Dockerfile .
	docker tag $(TAG):$(shell cat .make/.version) $(DOCKER_DOMAIN)/$(TAG):$(shell cat .make/.version)
	docker push $(DOCKER_DOMAIN)/$(TAG):$(shell cat .make/.version)
	@touch $@

.make/.push-updater: k8s/updater/Dockerfile
	docker build -t $(TAG)-updater:$(shell cat .make/.version) --platform=linux/amd64 -f k8s/updater/Dockerfile .
	docker tag $(TAG)-updater:$(shell cat .make/.version) $(DOCKER_DOMAIN)/$(TAG)-updater:$(shell cat .make/.version)
	docker push $(DOCKER_DOMAIN)/$(TAG)-updater:$(shell cat .make/.version)
	@touch $@

.PHONY: push
push:   .make/.push-server .make/.push-updater

.PHONY:clean
clean:
	rm -rf .make
	mvn clean

.PHONY:version-update
version-update:
	mvn versions:set && mvn versions:commit

.PHONY: help
help:
	@echo "make-tools for $(TAG)"
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  build           to test and build the project"
	@echo "  build-server    to test and build just the server"
	@echo "  build-client    to test and build just the client"
	@echo "  run-server      to start the server app"
	@echo "  docker-run      to start the server app in docker"
	@echo "  docker-stop     to stop the server app in docker"
	@echo "  run-env         to run the annorepo env command"
	@echo "  docker-image    to build the docker image of the app"
	@echo "  push            to push the linux/amd64 docker image to registry.diginfra.net"
	@echo "  clean           to remove generated files"
	@echo "  version-update  to update the project version"
	@echo
