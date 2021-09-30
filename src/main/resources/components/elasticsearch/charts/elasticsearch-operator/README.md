# 安装Elasticsearch Operator
```
helm upgrade --install elasticsearch-operator . --namespace <YOUR-NAMESPACE>
```
# 卸载Elasticsearch Operator
```
helm delete elasticsearch-operator --namespace <YOUR-NAMESPACE>
```
# Elasticsearch Operator chart 的配置说明

|  参数|  描述| 默认值 |
| --- | --- | --- |
| replicaCount | 实例数 | 1 |
| image.repository | 镜像地址 | harmonyware.harbor.cn/middleware |
| image.pullPolicy | 镜像的拉取策略 | IfNotPresent |
| image.tag | 镜像的tag号 | v1.3.3 |
| imagePullSecrets | 镜像拉取的secret | [] |
| nameOverride | 重命名chart的名称 |  |
| fullnameOverride |  |  |
| podAnnotations | 设置pod 的annotation | {} |
| podSecurityContext | 设置Pod 的SecurityContext | 见values.yaml |
| securityContext | 设置容器的SecurityContext | 见values.yaml |
| resources | 设置Pod 的资源 | 见values.yaml |
| tolerations | 配置Pod容忍 | [] |
| affinity | 配置亲和 | {} |