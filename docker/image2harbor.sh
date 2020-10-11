#!/bin/sh

if [ $# -lt 1 ]; then
    echo "Usage: $0 {dockerfile-name}"
    echo "push image to harbor."
    exit 100;
fi

dname=`dirname $0`
dname=`cd $dname&&pwd`
dockerfile=$dname/$1
harbor=imgregistry:28889
image=library/redis-manager:latest
remoteimg=$harbor/$image

cd $dname/

docker rmi $image
docker rmi $remoteimg

docker build --no-cache --tag ${image} -f $dockerfile .

docker tag ${image} $remoteimg

docker push $remoteimg

docker rmi $remoteimg
docker rmi $image

