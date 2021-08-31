#!/bin/bash
# harbor参数
harborAddress=10.1.10.14:8443
harborRepo=middleware
harborUser=admin
harborPassword=Harbor12345
# 镜像参数
image=zeus
tag=v1.1.0

# 先进到项目目录
cd ..

# 编译
echo "mvn clean package -DskipTests"
mvn clean package -DskipTests

echo ""

# 构建镜像
echo "docker build -t $harborAddress/$harborRepo/$image:$tag ."
docker build -t $harborAddress/$harborRepo/$image:$tag .

echo ""

# 登录harbor & 推送
# echo "docker login $harborAddress -u $harborUser -p $harborPassword"
# docker login $harborAddress -u $harborUser -p $harborPassword
# echo "docker push $harborAddress/$harborRepo/$image:$tag"
# docker push $harborAddress/$harborRepo/$image:$tag
# echo ""

# 删除本地镜像
echo "docker rmi $harborAddress/$harborRepo/$image:$tag"
docker rmi $harborAddress/$harborRepo/$image:$tag
