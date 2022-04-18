bashpath=$1
harbor=$2

for file in $bashpath/*.tar; do
    old_full_image=`docker load -i $file | grep Loaded | awk '{print $3}'`
    for full_image in $old_full_image; do
       image=`echo $full_image | awk -F '[/]' '{print $NF}'`
       echo $image
       docker tag $full_image $harbor/middleware/$image
       docker push $harbor/middleware/$image &
    done
done

wait