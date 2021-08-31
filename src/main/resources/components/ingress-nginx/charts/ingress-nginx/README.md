# kube-proxy的ipvs模式下的配置说明
```
configmap kube-proxy中ipvs下设置 excludeCIDRs: [0.0.0.0/0]，来保证ipvs规则不被flush的
```
# 安装ingress-nginx
```
1. 给节点打标签，让ingress-nginx 只运行在一些高配置的节点上，然后在values.yaml中nodeSelector参数中指定所打的节点标签
2. helm upgrade --install ingress-nginx . --namespace <YOUR-NAMESPACE>
```
# 安装ingress-nginx, 同时启用keepalived, 满足ingress的高可用需求
```
1. 给节点打标签，让ingress-nginx 只运行在一些高配置的节点上，然后在values.yaml中nodeSelector参数中指定所打的节点标签
2. 启用keepalived, 修改参数keepalived.enable为true, 同时通过调整参数keepalived.vip 设置Ingress的vip, 与节点同一网段且没有被使用的IP
3. 启用负载均衡，默认keepalived.lvsmode为VIP，可以通过调整参数keepalived.lvsmode 设置为NAT或DR来实现负载均衡功能
4. helm upgrade --install ingress-nginx . --namespace <YOUR-NAMESPACE>
```
# 卸载ingress-nginx
```
helm delete ingress-nginx --namespace <YOUR-NAMESPACE>
```
# ingress chart 的配置说明

|  参数|  描述| 默认值 |
| --- | --- | --- |
| httpPort | 提供http服务的端口 | 80 |
| httpsPort | 提供https服务的端口 | 443 |
| ingressClass | Ingress Controller的唯一标示，创建Ingress资源时在annotation中添加ingressClass名称，表明该Ingress资源由哪个Ingress Controller 控制 | nginx-ingress-controller |
| workerProcesses | nginx worker数量 | 8 |
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
| hostNetwork | 当pod设置hostNetwork:true时候，Pod中的所有容器就直接暴露在宿主机的网络环境中，这时候，Pod的PodIP就是其所在Node的IP，可选false，这时Pod的PodIP是分配的IP | true |
| keepalived | 设置VIP，与节点同一网段且无冲突，满足ingress高可用需求。默认VIP模式，也提供负载均衡的功能，可选NAT、DR模式，其中DR模式下service中的port和targetPort必须一致 | 见values.yaml |

# ingress-nginx 运维说明
## 启用keepalived的场景
部署ingress-nginx 成功之后, 即可通过ping vip来验证vip 是否生效
