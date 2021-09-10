DEPLOY_TYPE=$1
IMAGE_REPO=$2
STORAGE_CLASS=$3

function upgrade_helm() {
  helm install --create-namespace -n zeus zeus deploy/helm --set global.repository=$IMAGE_REPO"/middleware",global.storageClass=$STORAGE_CLASS
}

function upgrade_compose() {
  cd deploy/docker-compose
  docker-compose -f zeus.yaml stop
  docker-compose -f zeus.yaml up -d
}

function upgrade_sql() {
    mysql -u root -pZeuS@Middleware01 < sql/upgrade/$TARGET_VERSION/upgrade.sql
}

if [ $DEPLOY_TYPE == "helm" ]; then
  upgrade_helm
fi

if [ $DEPLOY_TYPE == "docker-compose" ]; then
  upgrade_compose
fi

upgrade_sql