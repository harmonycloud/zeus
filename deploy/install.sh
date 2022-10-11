#!/bin/bash

DEPLOY_TYPE=$1
LINE_TYPE=$2
IMAGE_REPO=$3
STORAGE_CLASS=$4
HA=$5

if [ $LINE_TYPE == "offline" ]; then
  echo "######  Push images  ######"

  sh ./deploy/load-image.sh ./deploy $IMAGE_REPO

  echo "######  Push images done !  ######"
fi

function deploy_docker() {
  cd deploy/docker-compose/
  zeus_repository=$IMAGE_REPO"/" docker-compose -f zeus.yaml up -d
}

function deploy_kubernetes() {
  kubectl create -f zeus.yaml
  kubectl create -f zeus-ui.yaml
}

function deploy_helm() {

  kubectl apply -f deploy/namespaces.yaml
  MYSQL_REPLICATE="replicaCount=1"
  OPERATOR_HELM_SET=""
  MYSQL_HELM_SET=""
  if [ $HA == "ha" ]; then
    MYSQL_REPLICATE="replicaCount=2"
    OPERATOR_HELM_SET=" --set replicaCount=3"
  fi

  if [ $HA == "active-active" ]; then
    MYSQL_REPLICATE="replicaCount=2"
    OPERATOR_HELM_SET=" -f deploy/mysql-operator/charts/mysql-operator/values-active-active.yaml --set replicaCount=3"
    MYSQL_HELM_SET=" -f deploy/mysql-operator/values-active-active.yaml "
  fi
  # install mysql-operator
  helm install -n middleware-operator mysql-operator  deploy/mysql-operator/charts/mysql-operator --set image.repository=$IMAGE_REPO $OPERATOR_HELM_SET
  # install mysql instance
  helm install -n zeus zeus-mysql deploy/mysql-operator --set mysql-operator.enabled=false,image.repository=$IMAGE_REPO,args.root_password="ZeuS@Middleware01",storageClassName=$STORAGE_CLASS,$MYSQL_REPLICATE $MYSQL_HELM_SET
  # install zeus platform
  HELM_ARGS="global.replicaCount=1"
  if [ $HA == "ha" ]; then
    HELM_ARGS="global.replicaCount=3"
  fi
  if [ $HA == "active-active" ]; then
    HELM_ARGS="global.replicaCount=3 -f deploy/helm/values-active-active.yaml"
  fi
  helm install -n zeus zeus deploy/helm --set global.repository=$IMAGE_REPO,global.storageClass=$STORAGE_CLASS,$HELM_ARGS
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