#!/bin/bash

DEPLOY_TYPE=$1
LINE_TYPE=$2
IMAGE_REPO=$3
STORAGE_CLASS=$4

if [ $LINE_TYPE == "offline" ]; then
  echo "######  Load images ######"
  docker load -i ./deploy/zeus-offline.tar
  echo "######  Load images done !  ######"
  echo
  echo "######  Push images  ######"
  IMAGES=$(cat ./build/image.conf)
  for IMG in $IMAGES
  do
  {
    docker tag $IMG $IMAGE_REPO"/"$IMG
    docker push $IMAGE_REPO"/"$IMG
  }
  done
  echo "######  Push images done !  ######"
fi

function deploy_docker() {
  cd deploy/docker-compose/
  zeus_repository=$IMAGE_REPO"/middleware/" docker-compose -f zeus.yaml up -d
}

function deploy_kubernetes() {
  kubectl create -f zeus.yaml
  kubectl create -f zeus-ui.yaml
}

function deploy_helm() {
  helm install --create-namespace -n zeus zeus deploy/helm --set global.repository=$IMAGE_REPO"/middleware",global.zeus_mysql.storageClass=$STORAGE_CLASS
}

if [ $DEPLOY_TYPE == "docker-compose" ]; then
    echo "######  Deployed by docker-compose ######"
    deploy_docker
    echo "######  Success ######"
fi

if [ $DEPLOY_TYPE == "kubernetes" ]; then
    echo "######  Deploy by kubernetes ######"
    deploy_kubernetes
fi

if [ $DEPLOY_TYPE == "helm" ]; then
    echo "######  Deployed by docker-compose ######"
    deploy_helm
    echo "######  Success ######"
fi