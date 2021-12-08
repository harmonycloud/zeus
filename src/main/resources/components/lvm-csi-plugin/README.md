# 安装lvm-csi-plugin
```
helm upgrade --install lvm-csi-plugin . --namespace <YOUR-NAMESPACE>
```
# 卸载lvm-csi-plugin
```
helm delete lvm-csi-plugin --namespace <YOUR-NAMESPACE>
```
# lvm-csi-plugin chart 的配置说明

|  参数|  描述| 默认值 |
| --- | --- | --- |
| image | 配置集群镜像信息 | 见values.yaml |
| resources | 设置Pod 资源 | 见values.yaml |
| nameOverride | 重命名chart的名称 |  |
| fullnameOverride |  |  |
| podAnnotations | 设置pod 的annotation | {} |
| podSecurityContext | 设置Pod 的SecurityContext | 见values.yaml |
| securityContext | 设置容器的SecurityContext | 见values.yaml |
| resources | 设置Pod 的资源 | 见values.yaml |
| nodeSelector | 节点标签 | middleware: ingress |
| tolerations | 配置Pod容忍 | [] |
| affinity | 配置亲和 | {} |

