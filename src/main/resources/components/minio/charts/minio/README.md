# 安装单实例minio
```
1. 修改实例 replicas: 1
2. 安装
   helm upgrade --install minio . --namespace <YOUR-NAMESPACE>
```
# 安装多实例minio
```
1. 根据实际场景修改replicas实例数
2. 安装
   helm upgrade --install minio . --namespace <YOUR-NAMESPACE>
```
# 卸载minio
```
helm delete minio --namespace <YOUR-NAMESPACE>
```
# minio chart 的配置说明

|  参数|  描述| 默认值 |
| --- | --- | --- |
| image | MinIO镜像名称 | minio/minio |
| imageTag | MinIO镜像tag | RELEASE.2020-12-03T05-49-24Z |
| imagePullPolicy | Image pull policy | IfNotPresent |
| accessKey | 默认access key | minio |
| secretKey | 默认secret key | Hc@Cloud01 |
| configPath | 默认配置文件路径 | ~/.minio |
| mountPath | 默认挂载路径 | /export |
| persistence.size | 持久卷大小 | 5Gi |
| persistence.storageClass | 持久卷类型 | default |
| replicas | 实例数 | 4 |

# minio 运维说明
## minio客户端
MinIO Client (mc)为ls，cat，cp，mirror，diff，find等UNIX命令提供了一种替代方案。它支持文件系统和兼容Amazon S3的云存储服务（AWS Signature v2和v4）。
```
ls       列出文件和文件夹。
mb       创建一个存储桶或一个文件夹。
cat      显示文件和对象内容。
pipe     将一个STDIN重定向到一个对象或者文件或者STDOUT。
share    生成用于共享的URL。
cp       拷贝文件和对象。
mirror   给存储桶和文件夹做镜像。
find     基于参数查找文件。
diff     对两个文件夹或者存储桶比较差异。
rm       删除文件和对象。
events   管理对象通知。
watch    监听文件和对象的事件。
policy   管理访问策略。
session  为cp命令管理保存的会话。
config   管理mc配置文件。
update   检查软件更新。
version  输出版本信息。
admin    管理配置
```
##下载二进制文件
https://dl.min.io/client/mc/release/linux-amd64/mc

##配置客户端环境
从MinIO服务获得URL、access key和secret key。
```
mc config host add minio http://URL  access_key secret_key #配置客户端环境
mc ls minio               #查看minio
mc mb minio/test-minio    #添加一个名为test-minio的bucket
mc cp 1 minio/test-minio  #将文件拷贝至minio
mc du minio
mc admin info minio
mc admin bucket quota myminio/mybucket --hard 64MB #配置bucket大小限制
mc admin bucket quota myminio/mybucket --clear     #取消bucket大小限制
```

##minio升级
MinIO 服务端支持滚动升级, 也就是说你可以一次更新分布式集群中的一个MinIO实例。 这样可以在不停机的情况下进行升级。可以通过将二进制文件替换为最新版本并以滚动方式重新启动所有服务器来手动完成升级。但是, 我们建议所有用户从客户端使用 mc admin update 命令升级。 这将同时更新集群中的所有节点并重新启动它们, 如下命令所示:
```
mc admin update <minio alias, e.g., myminio>
```
注意: 有些发行版可能不允许滚动升级，这通常在发行说明中提到，所以建议在升级之前阅读发行说明。在这种情况下，建议使用mc admin update升级机制来一次升级所有服务器。
MinIO升级时要记住的重要事项

• mc admin update 命令仅当运行MinIO的用户对二进制文件所在的父目录具有写权限时才工作, 比如当前二进制文件位于/usr/local/bin/minio, 你需要具备/usr/local/bin目录的写权限.
• mc admin update 命令同时更新并重新启动所有服务器，应用程序将在升级后重试并继续各自的操作。
• mc admin update 命令在 kubernetes/container 环境下是不能用的, 容器环境提供了它自己的更新机制来更新。
• 对于联盟部署模式，应分别针对每个群集运行mc admin update。 在成功更新所有群集之前，不要将mc更新为任何新版本。
• 如果将kes用作MinIO的KMS，只需替换二进制文件并重新启动kes，可以在 这里 找到有关kes的更多信息。
• 如果将Vault作为MinIO的KMS，请确保已遵循如下Vault升级过程的概述：https://www.vaultproject.io/docs/upgrading/index.html
• 如果将MindIO与etcd配合使用, 请确保已遵循如下etcd升级过程的概述: https://github.com/etcd-io/etcd/blob/master/Documentation/upgrades/upgrading-etcd.md