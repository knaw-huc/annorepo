all: help
TAG = annorepo
DOCKER_DOMAIN = registry.diginfra.net/tt
SHELL=/bin/bash
CLIENT_SRC=$(shell find client/src/ -type f)
COMMON_SRC=$(shell find common/src/ -type f)
version_fn = $(shell cat .make/.version 2>/dev/null)

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

.make/install-client: .make client/pom.xml $(CLIENT_SRC) common/pom.xml $(COMMON_SRC)
	mvn --projects client --also-make install
	@touch $@

.PHONY: install-client
install-client: .make/install-client

.PHONY: run-server-with-auth
run-server-with-auth: build-server
	@make start-mongodb
	AR_WITH_AUTHENTICATION=true AR_ROOT_API_KEY=root java -jar server/target/annorepo-server-$(call version_fn).jar server config.yml

.PHONY: run-server-without-auth
run-server-without-auth: build-server
	@make start-mongodb
	AR_WITH_AUTHENTICATION=false java -jar server/target/annorepo-server-$(call version_fn).jar server config.yml

.PHONY: run-env
run-env: build-server
	java -jar server/target/annorepo-server-$(call version_fn).jar env

.PHONY: docker-run
docker-run: k8s/local/docker-compose.yml
	cd k8s/local && docker compose up &

.PHONY: docker-stop
docker-stop: k8s/local/docker-compose.yml
	cd k8s/local && docker compose down

.make/.docker: .make/.version k8s/annorepo-server/Dockerfile-multistage
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
push:   clean build-server .make/.push-server .make/.push-updater

.PHONY: clean
clean:
	rm -rf .make */src/generated/*/*
	mvn clean

.PHONY: version-update
version-update:
	mvn versions:set && mvn versions:commit && find . -name dependency-reduced-pom.xml -delete
	make client/readme.md

.make/.deploy: build-client
	export GPG_TTY=$(tty)
	mvn clean install
	mvn --projects client --also-make deploy -P release
	open https://repo.maven.apache.org/maven2/io/github/knaw-huc/annorepo-client/
	open https://central.sonatype.com/search?q=annorepo
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

.PHONY: release
release:
	git pull && \
	git push && \
	make version-update && \
	git commit -a -m "bump version to $(call version_fn)" && \
	git push && \
	gh release create v$(call version_fn) && \
	make deploy

.PHONY: tests
tests:
	mvn test -Dmaven.plugin.validation=VERBOSE

.PHONY: start-mongodb
start-mongodb:
	docker start mongodb6 || docker run --name mongodb6 -d -p 27017:27017 -v ~/local/mongo:/data/db mongo:6.0.23

.PHONY: set-log-level-debug
set-log-level-debug:
	curl -X POST -d "logger=ROOT&level=DEBUG" http:/localhost:8081/tasks/log-level

.PHONY: set-log-level-info
set-log-level-info:
	curl -X POST -d "logger=ROOT&level=INFO" http:/localhost:8081/tasks/log-level

.make/compiled-protocol-buffers: .make common/src/main/proto/*.proto
	mkdir -p common/target/python
	python -m grpc_tools.protoc \
	-I common/src/main/proto \
	--python_out=common/target/python \
	--pyi_out=common/target/python \
	--grpc_python_out=common/target/python \
	common/src/main/proto/*.proto
#	protoc	--proto_path=common/src/main/proto \
#	--python_out=common/target/python \
#	--pyi_out=common/target/python \
#	--grpc_python_out=common/target/python \
#	common/src/main/proto/*.proto
	@touch $@

.PHONY: compile-protocol-buffers
compile-protocol-buffers: .make/compiled-protocol-buffers

current_branch=$(shell git branch --show-current)

.PHONY: git-pull
git-pull:
	git checkout main && git pull
	@echo
	git checkout develop && git merge main && git pull && git push
	#git checkout develop && git rebase main && git pull && git push
	@echo
	git checkout $(current_branch)

.PHONY: help
help:
	@echo "make-tools for $(TAG)"
	@echo
	@echo "Please use \`make <target>', where <target> is one of:"
	@echo "  git-pull                  - to pull git changes from the main branch ( + update the develop branch)"
	@echo "  tests                     - to test the project"
	@echo "  clean                     - to remove generated files"
	@echo "  compile-protocol-buffers  - to compile all .proto files"
	@echo "  install-client            - to install the client code in the local maven repository"
	@echo
	@echo "  build                     - to test and build the project"
	@echo "  build-server              - to test and build just the server"
	@echo "  build-client              - to test and build just the client"
	@echo
	@echo "  start-mongodb             - to start a local mongodb"
	@echo "  run-server-with-auth      - to start the server app with authorization on"
	@echo "  run-server-without-auth   - to start the server app with authorization off"
	@echo "  run-env                   - to run the annorepo env command"
	@echo "  set-log-level-debug       - to set the log level of the server app to DEBUG"
	@echo "  set-log-level-info        - to set the log level of the server app to INFO"
	@echo
	@echo "  docker-run                - to start the server app in docker"
	@echo "  docker-stop               - to stop the server app in docker"
	@echo
	@echo "  docker-image              - to build the docker image of the app (¹)"
	@echo "  push                      - to push the linux/amd64 docker image to registry.diginfra.net (¹)"
	@echo
	@echo "  version-update            - to update the project version"
	@echo "  dokka                     - to generate dokka html"
	@echo "  deploy                    - to deploy annorepo-client and annorepo-common"
	@echo "  release                   - to create a new release on github + deploy the new client"
	@echo
	@echo "¹) for test purposes only, the public docker image is built by github actions upon a release"
