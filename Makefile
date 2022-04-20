all: help
tag = annorepo

.PHONY: build run-server docker-image push help

.make:
	mkdir -p .make

.make/.version: .make pom.xml
	mvn help:evaluate -Dexpression=project.version -q -DforceStdout > .make/.version

target/annorepo-$(shell cat .make/.version).jar: .make/.version  $(shell find src -type f) pom.xml
	mvn package

build: .make/.version target/annorepo-$(shell cat .make/.version).jar

run-server: build
	java -jar target/annorepo-$(shell cat .make/.version).jar server config.yml

.make/.docker: .make build Dockerfile
	docker build -t $(tag):$(shell cat .make/.version) .
	@touch .make/.docker

docker-image: .make/.docker

.make/.push: .make/.docker
	docker tag $(tag):$(shell cat .make/.version) registry.diginfra.net/tt/$(tag):$(shell cat .make/.version)
	docker push registry.diginfra.net/tt/$(tag):$(shell cat .make/.version)
	@touch .make/.push

push:   .make/.push

init_db:
	psql -U postgres -f sql/init.sql

clean:
	rm -rf .make
	mvn clean

help:
	@echo "make-tools for $(tag)"
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  build           to test and build the app"
	@echo "  init_db         to create the necessary database and user"
	@echo "  run-server      to start the server app"
	@echo "  docker-image    to build the docker image of the app"
	@echo "  push            to push the docker image to registry.diginfra.net"
	@echo "  clean           to remove generated files"
