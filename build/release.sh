IMAGE_FILE=$1
REPO=$2
IMAGES=$(cat $IMAGE_FILE | grep -vE '^$|#')
RELEASE_PATH="release/"

rm -rf $RELEASE_PATH
mkdir $RELEASE_PATH

ALL_NAME=""

# image
mkdir $RELEASE_PATH"/images"
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

docker save $ALL_NAME > $RELEASE_PATH"/images/zeus.tar"

mkdir $RELEASE_PATH"/deploy"
cp deploy/* $RELEASE_PATH"/deploy/"

tar -cf release.tar $RELEASE_PATH