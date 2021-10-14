<p align="center">
    <img src="./docs/img/zeus-icon.svg" alt="Zeus" width="300" />
</p>

---
- 一个基于稳定k8s架构的中间件平台，定位开放、稳定、轻量；
- 除了支持常见的中间件外，如Redis、MySQL、ES、RockerMQ，还可以支持用户上架自定义的中间件；
- 可实现中间件应用自动扩缩容、故障自愈、自动备份及恢复、智能监控等。

## 平台优势
- **简单易用**：一条指令即可实现平台傻瓜式部署
- **兼容并包**：支持单主机、多主机等多场景部署形式，主机无障碍扩、缩容
- **稳定高效**：通过中间件服务实例及数据备份，快速恢复服务，保障中间件服务高可用
- **按需伸缩**：中间件服务多实例部署，按需伸缩，优化资源使用效率
- **全栈监控**：提供运行监控、事件告警、标准化日志等一条龙解决方案
- **简易运维**：支持问题发现及快速暴露，部分问题可实现无干预自我治愈

## 功能列表
- 中间件上架
- 发布中间件实例
- 对外访问配置
- 数据备份
- 日志搜集
- 实例监控&问题告警
- 用户&角色管理
- 资源管理


## Quick Start
版本要求:
- docker >= 18.09.9
- docker-compose >= 1.18.0
- kubernetes >= 1.17.2

Zeus平台使用docker-compose进行部署。

```
git clone https://github.com/harmonycloud/zeus.git
make install
```

访问方式

```
http://<your ip>:31088

用户名：admin
密码：Ab123456!
```

## 支持组件版本

|中间件|支持版本|支持模式|
|---|---|---|
|Mysql| 5.7.21|主从|
|Redis|5.0.8|哨兵、集群(三主三从、五主五从)|
|Elasticsearch|6.8.10-1|主从、主从协调|
|RocketMQ|4.8.0|双主、双主双从、三主三从|
|Kafka|2.13-2.6.0| |
|Zookeeper|3.6.1| |
|Minio|RELEASE.2021-02-14T04-01-33Z| |
|Nacos|1.4.1| |

## 平台组件

zeus-ui: https: https://github.com/harmonycloud/zeus-ui

redis-operator: https://github.com/harmonycloud/redis-operator

其余operator暂未开源

## 手册

用户手册：https://github.com/harmonycloud/zeus/blob/main/docs/user-guide/README.md

开发手册：https://github.com/harmonycloud/zeus/blob/main/docs/developer-guide/README.md

## License

Zeus is licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.