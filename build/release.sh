IMAGE_FILE=$1
REPO=$2
IMAGES=$(cat $IMAGE_FILE | grep -vE '^$|#')

ALL_NAME=""

# image
for IMG in $IMAGES
do
{
  echo "Docker pulling "$REPO"/"$IMG
  docker pull $REPO"/"$IMG
  docker tag $REPO"/"$IMG $IMG
  NAME=$(echo $IMG | sed 's/:/-/')
  ALL_NAME=$(echo $ALL_NAME" "$REPO"/"$IMG)
}
done

docker save $ALL_NAME > "./deploy/zeus-offline.tar"