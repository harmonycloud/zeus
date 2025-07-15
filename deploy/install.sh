#!/bin/bash

DEPLOY_TYPE=$1
LINE_TYPE=$2
IMAGE_REPO=$3
STORAGE_CLASS=$4
HA=$5
DISASTER=$6

if [ $LINE_TYPE == "offline" ]; then
  echo "######  Push images  ######"

  sh ./deploy/load-image.sh ./deploy $IMAGE_REPO

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
  # install lvm
  helm upgrade -i -n kube-system lvm-csi-plugin src/main/resources/components/lvm-csi-plugin --set image.repository=$IMAGE_REPO  -f src/main/resources/components/lvm-csi-plugin/values.yaml -f src/main/resources/components/lvm-csi-plugin/values-ha.yaml
  # install mysql-operator
  helm install -n middleware-operator mysql-operator deploy/mysql-operator/charts/mysql-operator --set image.repository=$IMAGE_REPO,replicaCount=3 -f deploy/mysql-operator/charts/mysql-operator/values.yaml -f deploy/mysql-operator/charts/mysql-operator/values-active-active.yaml
  # install mysql instance
  MYSQL_REPLICATE="replicaCount=1"
  if [ $HA == "true" ]; then
    MYSQL_REPLICATE="replicaCount=2"
  fi
  # install mysql type
  MYSQL_TYPE="type=master-slave"
  if [ $DISASTER == "slave" ]; then
      MYSQL_TYPE="type=slave-slave"
  fi
  helm install -n zeus zeus-mysql deploy/mysql-operator --set mysql-operator.enabled=false,image.repository=$IMAGE_REPO,args.root_password="ZeuS@Middleware01",storageClassName=$STORAGE_CLASS,storageSize=10Gi,$MYSQL_REPLICATE,$MYSQL_TYPE -f deploy/mysql-operator/values.yaml -f deploy/mysql-operator/values-active-active.yaml

  # install zeus platform
  HELM_ARGS="global.replicaCount=1"
  if [ $HA == "true" ]; then
    HELM_ARGS="global.replicaCount=3"
  fi
  helm install -n zeus zeus deploy/helm --set global.repository=$IMAGE_REPO,global.storageClass=$STORAGE_CLASS,$HELM_ARGS

  # 创建monitoring命名空间
  kubectl create ns monitoring
  # 获取etcd证书
  kubectl create secret generic etcd-certs --from-file=/etc/kubernetes/pki/etcd/healthcheck-client.crt --from-file=/etc/kubernetes/pki/etcd/healthcheck-client.key --from-file=/etc/kubernetes/pki/etcd/ca.crt -n monitoring --dry-run -oyaml  > src/main/resources/components/prometheus/templates/prometheus/etcd-certs.yaml
  # 安装prometheus
  helm install prometheus -n monitoring src/main/resources/components/prometheus --set prometheus.prometheusSpec.image.repository=$IMAGE_REPO/prometheus,kube-state-metrics.image.repository=$IMAGE_REPO/kube-state-metrics,prometheus-node-exporter.image.repository=$IMAGE_REPO/node-exporter,prometheusOperator.image.repository=$IMAGE_REPO/prometheus-operator,prometheusOperator.prometheusConfigReloader.image.repository=$IMAGE_REPO/prometheus-config-reloader,prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.storageClassName=$STORAGE_CLASS,prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=30Gi,prometheus.prometheusSpec.replicas=3 -f src/main/resources/components/prometheus/values.yaml
  # 安装alertmanager
  helm install alertmanager -n monitoring src/main/resources/components/alertmanager --set alertmanager.alertmanagerSpec.image.repository=$IMAGE_REPO/alertmanager,alertmanager.alertmanagerSpec.replicas=3 -f src/main/resources/components/alertmanager/values.yaml -f src/main/resources/components/alertmanager/values-active-active.yaml

  # 创建logging命名空间
  kubectl create ns logging
  # 安装es operator
  helm install elasticsearch-operator -n middleware-operator src/main/resources/components/elasticsearch/charts/elasticsearch-operator --set image.repository=$IMAGE_REPO,replicaCount=3 -f src/main/resources/components/elasticsearch/charts/elasticsearch-operator/values.yaml -f src/main/resources/components/elasticsearch/charts/elasticsearch-operator/values-active-active.yaml
  # 安装es
  helm install kubernetes-logging -n logging src/main/resources/components/elasticsearch --set image.repository=$IMAGE_REPO,aliasName=kubernetes-logging,nameOverride=kubernetes-logging,elasticsearch-operator.enabled=false,elasticPassword=Hc@Cloud01,storage.masterClass=$STORAGE_CLASS,storage.masterSize=30Gi,logging.collection.filelog.enable=false,logging.collection.stdout.enable=false,resources.master.limits.cpu=1,resources.master.limits.memory=4Gi,esJavaOpts.xmx=2048m,esJavaOpts.xms=2048m,cluster.masterReplacesCount=3,resources.master.requests.cpu=1,resources.master.requests.memory=4Gi -f src/main/resources/components/elasticsearch/values.yaml -f src/main/resources/components/elasticsearch/values-active-active.yaml
  # 安装log-pilot
  helm install log-pilot -n logging src/main/resources/components/log-pilot --set image.repository=$IMAGE_REPO,runtime.type=containerd -f src/main/resources/components/log-pilot/values.yaml -f src/main/resources/components/log-pilot/values-active-active.yaml
  # 安装logstash
  helm install logstash -n logging src/main/resources/components/logstash --set image=$IMAGE_REPO/logstash,replicas=2 -f src/main/resources/components/logstash/values.yaml -f src/main/resources/components/logstash/values-ha.yaml


  # 安装中间件控制器
  helm install middleware-controller -n middleware-operator src/main/resources/components/platform --set global.repository=$IMAGE_REPO -f src/main/resources/components/platform/values.yaml -f src/main/resources/components/platform/values-active-active.yaml
  # 安装备份控制器
  helm install middlewarebackup-controller -n middleware-operator src/main/resources/components/middleware-backup --set global.repository=$IMAGE_REPO -f src/main/resources/components/middleware-backup/values.yaml -f src/main/resources/components/middleware-backup/values-active-active.yaml
  # 安装middleware webhook
  helm install middleware-admission-webhook -n middleware-operator src/main/resources/components/middleware-admission-webhook --set image.repository=$IMAGE_REPO,replicaCount=3 -f src/main/resources/components/middleware-admission-webhook/values.yaml -f src/main/resources/components/middleware-admission-webhook/values-active-active.yaml
  # 安装fs exporter
  helm install fs-exporter -n middleware-operator src/main/resources/components/fs-exporter --set image.registry=$IMAGE_REPO -f src/main/resources/components/fs-exporter/values.yaml
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