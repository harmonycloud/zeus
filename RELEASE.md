### v0.1.0
1. 中间件用户角色及权限体系完善
2. 平台菜单调整各页面修改方案
    - 平台弱化、隐藏k8s集群概念，建立资源池
3. 通用能力提升（开发中）
    - 中间件通用备份与恢复：针对所有的中间件都有一套通用的备份和恢复的方案
    - 定时备份、立即备份
    - 支持编辑、删除
    - 恢复：服务层次
    - 支持纳管现有资源池
    - 中间件升级/下架：控制面组件和中间件实例都应支持版本升级；支持中间件下架; 中间建Operator 状态
4. 中间件新个性能力提升
    - ES冷节点支持：ES 新增一种冷节点类型，对不常用的数据进行归档
5. k8s底座监控大屏整合设计

### v0.0.3
- 优化部署
- Bug Fix

### v0.0.2
- 优化纳管集群适配
- Bug Fix

## v0.0.1
- 新增用户管理、审计管理功能
- 新增RocketMQ ACL权限认证功能
- 新增图表样式的集群资源概览
- mysql方面
  - 新增mysql灾备管理功能
  - 新增mysql一主多从功能