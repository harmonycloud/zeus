#!/bin/bash

DEPLOY_TYPE=$1
LINE_TYPE=$2
IMAGE_REPO=$3
STORAGE_CLASS=$4
HA=$5

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

  kubectl apply -f deploy/namespaces.yaml
  # install mysql-operator
  helm install -n middleware-operator mysql-operator deploy/mysql-operator/charts/mysql-operator --set image.repository=$IMAGE_REPO"/middleware"
  # install mysql instance
  helm install -n zeus zeus-mysql deploy/mysql-operator --set mysql-operator.enabled=false,image.repository=$IMAGE_REPO"/middleware",args.root_password="ZeuS@Middleware01",storageClassName=$STORAGE_CLASS
  # install zeus platform
  HELM_ARGS="global.replicaCount=1"
  if [ $HA == "true" ]; then
    HELM_ARGS="global.replicaCount=3"
    echo -n "请输入Zeus平台使用的共享存储服务名称:"
    read STORAGE_CLASS
  fi
  helm install -n zeus zeus deploy/helm --set global.repository=$IMAGE_REPO"/middleware",global.storageClass=$STORAGE_CLASS,$HELM_ARGS
}

if [ $DEPLOY_TYPE == "docker-compose" ]; then
    echo "######  Deployed by docker-compose ######"
    deploy_docker
fi

if [ $DEPLOY_TYPE == "kubernetes" ]; then
    echo "######  Deploy by kubernetes ######"
    deploy_kubernetes
fi

if [ $DEPLOY_TYPE == "helm" ]; then
    echo "######  Deployed by helm ######"
    deploy_helm
fi

echo "######  Deploy done!  ######"