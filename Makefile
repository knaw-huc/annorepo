all: help
tag = annorepo

.make:
	mkdir -p .make

.make/.version: .make pom.xml
	mvn help:evaluate -Dexpression=project.version -q -DforceStdout > .make/.version

target/annorepo-$(shell cat .make/.version).jar: .make/.version  $(shell find src -type f) pom.xml
	mvn package

.PHONY: build
build: .make/.version target/annorepo-$(shell cat .make/.version).jar

.PHONY: run-server
run-server: build
	java -jar target/annorepo-$(shell cat .make/.version).jar server config.yml

.PHONY: run-env
run-env: build
	java -jar target/annorepo-$(shell cat .make/.version).jar env

.PHONY: docker-run
docker-run: k8s/local/docker-compose.yml
	cd k8s/local && docker compose up &

.PHONY: docker-stop
docker-stop: k8s/local/docker-compose.yml
	cd k8s/local && docker compose down

.make/.docker: .make build k8s/annorepo-server/Dockerfile
	docker build -t $(tag):$(shell cat .make/.version) -f k8s/annorepo-server/Dockerfile .
	@touch .make/.docker

.PHONY: docker-image
docker-image: .make/.docker

.make/.push: .make/.docker
	docker tag $(tag):$(shell cat .make/.version) registry.diginfra.net/tt/$(tag):$(shell cat .make/.version)
	docker push registry.diginfra.net/tt/$(tag):$(shell cat .make/.version)
	@touch .make/.push

.PHONY: push
push:   .make/.push

.PHONY:clean
clean:
	rm -rf .make
	mvn clean

.PHONY:version-update
version-update:
	mvn versions:set && mvn versions:commit

.PHONY: help
help:
	@echo "make-tools for $(tag)"
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  build           to test and build the app"
	@echo "  run-server      to start the server app"
	@echo "  docker-run      to start the server app in docker"
	@echo "  docker-stop     to stop the server app in docker"
	@echo "  run-env         to run the annorepo env command"
	@echo "  docker-image    to build the docker image of the app"
	@echo "  push            to push the docker image to registry.diginfra.net"
	@echo "  clean           to remove generated files"
	@echo "  version-update  to update the project version"
	@echo
