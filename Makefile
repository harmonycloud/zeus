IMAGE_REPO ?= harbor.middleware.cn:58080
TYPE ?= docker-compose
DEPLOY ?= online
STORAGE_CLASS ?= default
HA ?= false

release:
	build/release.sh build/image.conf $(IMAGE_REPO)

build:
	mvn clean install -DskipTests

image:
	make build && docker build -t zeus:v1.0.0 .

install:
	chmod +x ./deploy/install.sh && sh deploy/install.sh $(TYPE) $(DEPLOY) $(IMAGE_REPO) $(STORAGE_CLASS) $(HA)

upgrade:
	sh build/upgrade.sh $(TYPE) $(IMAGE_REPO) $(STORAGE_CLASS)