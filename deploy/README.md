# 部署文档

描述Zeus中间件平台部署方式

## 1.Docker-Compose

Docker-Compose为平台默认部署方式，不需要依赖K8s环境即可完成一键部署。

部署命令：
```
git clone https://github.com/harmonycloud/zeus.git
cd zeus && make install
```

访问方式

```
http://<your ip>:31088

用户名：admin
密码：zeus123.com
```

## Helm

将平台通过helm的方式部署进K8s集群。

**特别需要注意的是**：
1. 非高可用部署依赖K8s集群中的任意StorageClass
2. 高可用部署时平台依赖集群中的网络共享类型的StorageClass，如NFS，GlusterFS等

部署命令：
```
git clone https://github.com/harmonycloud/zeus.git -b v0.0.3
cd zeus
make install TYPE=helm STORAGE_CLASS=<k8s sc>
```

Helm部署方式make命令支持参数列表

| 参数名 | 描述 | 可填项 | 默认值 |
| --- | --- | --- | --- | --- |
| IMAGE_REPO | 指定平台部署镜像的仓库 | 镜像仓库名 | http://middleware.harmonycloud.cn:38080 |
| TYPE | 指定部署类型 | docker-compose/helm | docker-compose |
| DEPLOY | 指定在线/离线部署 | online/offline | online |
| STORAGE_CLASS | helm部署在中给数据库和平台后端使用的storageclass名称 | StorageClassName | default |
