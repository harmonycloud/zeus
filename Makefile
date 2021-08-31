IMAGE_REPO ?=

release:
	build/release.sh build/image.conf $(IMAGE_REPO)

build:
	mvn clean install -DskipTests

image:
	make build && docker build -t zeus:v1.0.0 .

install:
	chmod +x ./deploy/install.sh && sh deploy/install.sh  docker-compose online
