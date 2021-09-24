# 安装Mysql Operator
```
helm upgrade --install mysql-operator . --namespace <YOUR-NAMESPACE>
```
# 卸载Mysql Operator
```
helm delete mysql-operator --namespace <YOUR-NAMESPACE>
```
# Mysql Operator chart 的配置说明

|  参数|  描述| 默认值 |
| --- | --- | --- |
| operatorpwd | 在operator连接Mysql集群时，operator账号所使用的密码 | abcd!@#$ |
| replicapwd | 在主从同步时，同步账号replic所使用的密码 | abcd!@#$  |
| sshpwd | 在ssh 到容器中运维时，root账号所使用的密码 | abcd!@#$  |
| mysqlUserHost| 授权业务用户的源IP范围 | % |
| replicaCount | 实例数 | 1 |
| image.repository | 镜像地址 | harmonyware.harbor.cn/middleware |
| image.pullPolicy | 镜像的拉取策略 | IfNotPresent |
| image.tag | 镜像的tag号 | v1.0.9 |
| imagePullSecrets | 镜像拉取的secret | [] |
| nameOverride | 重命名chart的名称 |  |
| fullnameOverride |  |  |
| podAnnotations | 设置pod 的annotation | {} |
| podSecurityContext | 设置Pod 的SecurityContext | 见values.yaml |
| securityContext | 设置容器的SecurityContext | 见values.yaml |
| resources | 设置Pod 的资源 | 见values.yaml |
| tolerations | 配置Pod容忍 | [] |
| affinity | 配置亲和 | {} |