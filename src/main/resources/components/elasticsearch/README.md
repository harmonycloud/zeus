# 安装Elasticsearch集群
```
helm upgrade --install elasticsearch . --namespace <YOUR-NAMESPACE>
```
# 卸载Elasticsearch集群
```
helm delete elasticsearch --namespace <YOUR-NAMESPACE>
```
# 集群chart 的配置说明

|  参数|  描述| 默认值 |
| --- | --- | --- |
| elasticPassword | elastic用户的密码 | Hc@Cloud01 |
| storageSize | 配置存储的大小 | 5G |
| storageClassName | 配置存储的storageClass名称 | default |
| image | 配置集群镜像信息 | 见values.yaml |
| cluster.mode | 集群的类型，分为simple(节点充当master和data两种角色), regular(master和data分离部署), complex（节点分为master,data和client三种角色）三种 | 见values.yaml |
| cluster.masterReplacesCount | master类型节点的实例数 | 3 |
| cluster.dataReplacesCount | data类型节点的实例数 | 3 |
| cluster.clientReplacesCount | client类型节点的实例数 | 1 |
| esJavaOpts | 设置Java 堆大小 | Java 堆大小建议设置Pod 内存limit 的50% |
| resources | 设置Pod 资源 | 见values.yaml |

# 集群CR 的配置说明

```
apiVersion: es.middleware.hc.cn/v1alpha1
kind: ESCluster
metadata:
  annotations:
    OperationType: Create
    developerPass: es
    meta.helm.sh/release-name: es
    meta.helm.sh/release-namespace: middleware-test
  creationTimestamp: "2020-11-26T12:08:02Z"
  generation: 2
  labels:
    app.kubernetes.io/managed-by: Helm
  name: es-elasticsearch  #集群名称
  namespace: middleware-test #集群分区
spec:
  checkHealthConfigMap: es-elasticsearch-checkhealth-config #健康检查的配置文件
  clusterMode: simple #集群的类型，分为simple(节点充当master和data两种角色), regular(master和data分离部署), complex（节点分为master,data和client三种角色）
  elasticBusyImage: harmonyware.harbor.cn/middleware/busybox:latest #busybox 镜像地址
  elasticKibanaConfigMap: es-elasticsearch-kibana-config #kibana 配置文件
  elasticPass: Hc@Cloud01 #elastic用户的密码
  elasticSearchConfigMap: es-elasticsearch-es-config #es的配置文件
  esExporterPort: 19114 # es exporter 的端口
  esHttpPort: 9200 #es http 端口
  esKibanaPort: 5200 #kibana 端口
  esTcpPort: 9300 #es tcp端口
  kibanaImage: harmonyware.harbor.cn/middleware/kibana:v5.6.4-hc #kibana镜像地址
  masterReplaces: 3 #master类型节点的实例数
  dataReplaces: 0 #data类型节点的实例数
  clientReplaces: 0  #client类型节点的实例数
  pod:
  - env:
    - name: TZ
      value: Asia/Shanghai
    initImage: harmonyware.harbor.cn/middleware/es-init:v1.3 #init 镜像地址
    jvm:  #设置Java 堆大小，建议设置Pod 内存limit 的50%
      dataXmx: 2g
      masterXmx: 2g
    middlewareImage: harmonyware.harbor.cn/middleware/elastic:6.8.10-4 #es 的镜像地址
    monitorImage: harmonyware.harbor.cn/middleware/es-exporter:1.1.0rc1 #es exporter的镜像地址
    resources: # es 三种类型以及kibana的资源配置
      client:
        limits:
          cpu: "1"
          memory: 4Gi
        requests:
          cpu: "1"
          memory: 4Gi
      data:
        limits:
          cpu: "1"
          memory: 4Gi
        requests:
          cpu: "1"
          memory: 4Gi
      kibana:
        limits:
          cpu: 500m
          memory: 1Gi
        requests:
          cpu: 500m
          memory: 1Gi
      master:
        limits:
          cpu: "1"
          memory: 4Gi
        requests:
          cpu: "1"
          memory: 4Gi
    updateStrategy: {}
  projectId: a9927d1581674f27
  repository: harmonyware.harbor.cn/middleware #镜像repository信息
  totalReplaces: 3 #ES 三种类型的实例数之和
  type: simple #集群的类型
  updateStrategy: {}
  version: 6.8.10 # es 的版本
  volumeClaimTemplates: #es 存储信息
  - metadata:
      creationTimestamp: null
      name: es-data
    spec:
      accessModes:
      - ReadWriteOnce
      dataSource: null
      resources:
        requests:
          storage: 5G
      storageClassName: default
    status: {}
  volumes: {}
```

