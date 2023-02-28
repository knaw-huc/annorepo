all: help
TAG = annorepo
DOCKER_DOMAIN = registry.diginfra.net/tt
version_fn = $(shell cat .make/.version)

.make:
	mkdir -p .make

.make/.version: .make pom.xml
	mvn help:evaluate -Dexpression=project.version -q -DforceStdout > .make/.version

server/target/annorepo-server-$(call version_fn).jar: .make/.version  $(shell find server/src common/src -type f) pom.xml server/pom.xml
	mvn --projects server --also-make package

client/target/annorepo-client-$(call version_fn).jar: .make/.version  $(shell find client/src common/src -type f) pom.xml client/pom.xml
	mvn --projects client --also-make package

.PHONY: build
build: .make/.version server/target/annorepo-server-$(call version_fn).jar client/target/annorepo-client-$(call version_fn).jar

.PHONY: build-server
build-server: .make/.version server/target/annorepo-server-$(call version_fn).jar

.PHONY: build-client
build-client: .make/.version client/target/annorepo-client-$(call version_fn).jar client/readme.md

.PHONY: run-server
run-server: build-server
	java -jar server/target/annorepo-server-$(call version_fn).jar server config.yml

.PHONY: run-env
run-env: build-server
	java -jar server/target/annorepo-server-$(call version_fn).jar env

.PHONY: docker-run
docker-run: k8s/local/docker-compose.yml
	cd k8s/local && docker compose up &

.PHONY: docker-stop
docker-stop: k8s/local/docker-compose.yml
	cd k8s/local && docker compose down

.make/.docker: .make k8s/annorepo-server/Dockerfile-multistage
	docker build -t $(TAG):$(call version_fn) -f k8s/annorepo-server/Dockerfile-multistage .
	@touch $@

.PHONY: docker-image
docker-image: .make/.docker

.make/.push-server: build k8s/annorepo-server/Dockerfile
	docker build -t $(TAG):$(call version_fn) --platform=linux/amd64 -f k8s/annorepo-server/Dockerfile .
	docker tag $(TAG):$(call version_fn) $(DOCKER_DOMAIN)/$(TAG):$(call version_fn)
	docker push $(DOCKER_DOMAIN)/$(TAG):$(call version_fn)
	@touch $@

.make/.push-updater: k8s/updater/Dockerfile
	docker build -t $(TAG)-updater:$(call version_fn) --platform=linux/amd64 -f k8s/updater/Dockerfile .
	docker tag $(TAG)-updater:$(call version_fn) $(DOCKER_DOMAIN)/$(TAG)-updater:$(call version_fn)
	docker push $(DOCKER_DOMAIN)/$(TAG)-updater:$(call version_fn)
	@touch $@

.PHONY: push
push:   .make/.push-server .make/.push-updater

.PHONY: clean
clean:
	rm -rf .make
	mvn clean

.PHONY: version-update
version-update:
	mvn versions:set && mvn versions:commit && find . -name dependency-reduced-pom.xml -delete
	make client/readme.md

.make/.deploy: build-client
	export GPG_TTY=$(tty)
	mvn install
	mvn --projects client --also-make dokka:javadocJar
	mvn --projects client --also-make package gpg:sign deploy -P release
	@touch $@

.PHONY: deploy
deploy:	.make/.deploy

.make/.dokka: $(shell find */src -type f) pom.xml */pom.xml
	mvn dokka:dokka --projects client --also-make
	rm -rf client/dokka
	mv client/target/dokka client/dokka
	rm -rf common/dokka
	mv common/target/dokka common/dokka
	@touch $@

.PHONY: dokka
dokka:	.make/.dokka

client/readme.md: client/src/test/resources/readme.md client/pom.xml
	mvn --projects client --also-make resources:testResources
	echo "<!--- DO NOT EDIT THIS FILE! This file is generated from client/src/test/resources/readme.md; edit that file instead. -->" > client/readme.md
	cat client/target/test-classes/readme.md >> client/readme.md

.PHONY: help
help:
	@echo "make-tools for $(TAG)"
	@echo
	@echo "Please use \`make <target>', where <target> is one of:"
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
	@echo "  deploy          to deploy annorepo-client and annorepo-common"
	@echo "  dokka           to generate dokka html"
	@echo
