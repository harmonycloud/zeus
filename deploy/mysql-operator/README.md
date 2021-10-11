# 安装Mysql集群
```
helm upgrade --install mysql . --namespace <YOUR-NAMESPACE>
```
# 卸载Mysql集群
```
helm delete mysql --namespace <YOUR-NAMESPACE>
```
# 集群chart 的配置说明

|  参数|  描述| 默认值 |
| --- | --- | --- |
| root_password | root用户的密码 | Hc@Cloud01 |
| businessDeploy | 添加业务数据库，并设置用户名和密码 | 见values.yaml |
| init_at_setup | 支持集群启动时配置初始化脚本 | 见values.yaml |
| storage | 配置集群的存储 | 见values.yaml |
| replicaCount | 实例数 | 2 |
| image | 配置集群镜像信息 | 见values.yaml |
| resources | 设置Pod 资源 | 见values.yaml |