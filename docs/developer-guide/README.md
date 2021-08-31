# Zeus Middleware Platform
> Kubernetes中间件平台项目开发者指南

## 1. Zeus 项目结构
```
|── deploy                                                  # 部署文件
|── image-build                                             # 镜像构建相关
|── mysql                                                   # mysql sql文件
|── src/                                  
|   |── main/                                               # 源码目录
|       |── java/                                           # 源码
|           |── com.harmonycloud.zeus/           # 基础包名
|               |── annotation/                             # 自定义注解
|               |── bean/                                   # 数据库实体类
|               |── config/                                 # 自动装配
|                   |── FeignConfig.java                    # feign配置
|                   |── FiltersConfiguration.java           # 过滤器配置
|                   |── K8sEnvironment.java                 # k8s环境变量配置
|                   |── SwaggerConfig.java                  # swagger配置
|                   |── WebMvcConfig.java                   # mvc配置
|               |── controller/                             # controller层
|                   |── auth/                               # 认证授权相关
|                   |── k8s/                                # 直接调用k8s相关
|                   |── middleware/                         # 中间件业务相关
|               |── dao/                                    # mapper
|               |── generator/                              # 逆向生成
|               |── handler/                                # 异常处理
|                   |── GlobalExceptionHandler.java         # 统一异常处理
|               |── integration/                            # 封装其他项目/系统的接口调用
|                   |── cluster/                            # 封装k8s集群的接口调用
|                       |── api/                            # k8s集群的接口调用
|                       |── bean/                           # k8s集群接口实体类
|                       |── client/                         # k8s集群客户端
|                       |── xxxWrapper.java                 # 封装api/client的接口调用
|                   |── harbor/                             # 封装harbor的接口调用
|                       |── 同cluster
|                   |── minio/                              # 封装minio的接口调用
|               |── interceptor/                            # 拦截器
|                   |── ZeusInterceptor.java      # 拦截器
|               |── operator/                               # operator层，service的下一层
|                   |── api/                                # 中间件的operator接口（单个中间件不通用的功能接口写在此处）
|                   |── impl/                               # 中间件的operator接口的实现类（继承miiddleware目录的抽象类，只需要覆盖当前中间件的差异部分，通用的处理在父类里）
|                   |── miiddleware/                        # 中间件的operator接口的抽象类（继承AbstractBaseOperator类，同个中间件的不同版本的相同处理放在这里）
|                   |── AbstractBaseOperator.java           # 中间件业务通用的处理，差异之处由子类进行覆盖（模板设计模式）
|                   |── BaseOperator.java                   # 中间件业务通用的接口，通用实现放在AbstractBaseOperator里
|               |── schedule/                               # 定时任务
|                   |── ExecutorConfig.java                 # Spring方式的线程池配置类
|                   |── MiddlewareManageTask.java           # 中间件管理的异步任务
|               |── service/                                # 业务处理
|                   |── auth/                               # 认证授权
|                   |── k8s/                                # 处理k8s的调用
|                   |── middleware/                         # 处理中间件的业务
|                       |── AbstractMiddlewareService.java  # 中间件的通用处理抽象类
|                   |── registry/                           # 处理制品服务的调用
|                   |── AbstractBaseService.java            # service层的抽象父类，封装简单工厂方法来获取对应的中间件operatorImpl类
|               |── util/                                   # 工具类
|                   |── SpringContextUtils.java             # 获取SpringContex
|               |── ZeusApplication.java          # SpringBoot启动类
|       |── resource/                                       # 资源
|           |── config/                                     # 配置文件目录
|           |── mappiing/                                   # xml文件目录
|           |── logback.xml                                 # logback配置文件
|           |── sql.code.generator.properties               # mybatis-plus逆向生成的数据源配置文件
|   |── test/                                               # 测试目录
|       |── java/                                           # 测试源码
|       |── resource/                                       # 测试资源
|── Dockerfile                                              # dockerfile
|── pom.xml                                                 # pom文件
|── README.md                                               # 说明文档
```

## 2. 说明
### 2.1 配置说明
application-dev.yaml
```yaml
k8s:
  service:
    # 默认fabric8客户端的地址，有默认值，仅开发使用
    host: 127.0.0.1
    port: 6443
  token:
    # debug时填写本地的token文件路径，有默认值，仅开发使用
    path: /MyPath/token # change me
  kubeconfig:
    # debug时填写本地的kube config文件路径，有默认值，仅开发使用
    path: /MyPath/config
system:
  upload:
    # debug时填写自己本地的一个可访问的目录，仅开发使用，默认值：/usr/local/zeus-pv/upload
    path: /MyPath/upload
```
其中：
- `k8s.token.path`：需要在本地存放一个能访问`k8s`集群（看`k8s.service.*`配置）的`token`文件
- `k8s.kubeconfig.path`：填写本地一个IDE有权限访问的目录，初始化时会在该目录下写出`kubeconfig`文件
- `system.upload.path`：填写本地一个IDE有权限访问的目录，会往里面写入一些文件

### 2.2 本地debug所需工具
#### 2.2.2 `helm`
> 中间件发布和查询使用`helm`发布，需要`helm`命令，请自行下载

### 2.3 项目说明
#### 2.3.1 查数据库
> controller -> service -> mapper

#### 2.3.2 调其他系统
> controller -> service -> operator -> integration

其中：

`service`层的`middleware`目录下的实现类都会继承`AbstractBaseService`，它里面通过简单工厂获取对应的`xxxOperatorImpl`类（通过`@Operator`注解从`SpringContext`取到`bean`实例，再反射执行其`support`方法）
```java
public abstract class AbstractBaseService {
    /**
     * 内部类保证线程安全的单例
     */
    private static class Singleton {
        private static final Map<String, Object> OPERATORS = SpringContextUtils.getBeansWithAnnotation(Operator.class);
    }
    
    protected <T, R> T getOperator(Class<T> funClass, Class<R> baseClass, Object... types) {
        for (Map.Entry<String, Object> entry : Singleton.OPERATORS.entrySet()) {
            try {
                if (baseClass.isAssignableFrom(entry.getValue().getClass()) && types.length == 0
                    || (funClass.isAssignableFrom(entry.getValue().getClass()) && (boolean)baseClass
                        .getMethod("support",
                            entry.getValue().getClass().getAnnotation(Operator.class).paramTypes4One())
                        .invoke(entry.getValue(), types))) {
                    return (T)entry.getValue();
                }
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
```

`operator`层主要使用了模板模式：
##### 对于通用的方法处理
对于通用的增删改查、主从切换等，都定义在`BaseOperator`接口里，并且在`AbstractBaseOperator`里去进行通用的实现，差异之处由对应的中间件子类去进行`@Override`覆盖
```java
public interface BaseOperator {

    boolean support(Middleware middleware);

    void create(Middleware middleware, MiddlewareClusterDTO cluster);

    void update(Middleware middleware, MiddlewareClusterDTO cluster);

    void delete(Middleware middleware);

    void switchMiddleware(Middleware middleware);
}
```
`AbstractBaseOperator`类里放通用接口的通用处理，如中间件创建：
```java
public abstract class AbstractBaseOperator {

    public void create(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (cluster == null) {
            cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        }
        // 1. download and read helm chart from registry
        HelmChartFile helmChart = helmChartService.getHelmChartFromRegistry(cluster.getRegistry(),
            middleware.getChartName(), middleware.getChartVersion());

        // 2. deal with values.yaml andChart.yaml
        // load values.yaml to map
        Yaml yaml = new Yaml();
        JSONObject values = yaml.loadAs(helmChart.getValueYaml(), JSONObject.class);
        // deal with values.yaml file
        replaceValues(middleware, cluster, values);
        // deal with Charts.yaml file
        replaceChart(helmChart, values);
        // map to yaml
        String newValuesYaml = yaml.dumpAsMap(values);
        helmChart.setValueYaml(newValuesYaml);
        // write to local file
        helmChartService.coverYamlFile(helmChart);

        // 3. helm package & install
        String tgzFilePath = helmChartService.packageChart(helmChart.getTarFileName(), middleware.getChartName(),
            middleware.getChartVersion());
        helmChartService.install(middleware, tgzFilePath, cluster);

        // 4. 创建对外访问
        if (!CollectionUtils.isEmpty(middleware.getIngresses())) {
            try {
                middleware.getIngresses().forEach(ingress -> ingressService.create(middleware.getClusterId(),
                        middleware.getNamespace(), middleware.getName(), ingress));
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，中间件：{}，创建对外访问异常", middleware.getClusterId(), middleware.getNamespace(),
                        middleware.getName(), e);
                throw new BusinessException(ErrorMessage.MIDDLEWARE_SUCCESS_INGRESS_FAIL);
            }
        }
    }

}
```
子类可以覆盖创建时的`replaceValues()`方法
```java
public class MysqlOperatorImpl extends AbstractMysqlOperator implements MysqlOperator {

    @Override
    public void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用的值
        replaceCommonValues(middleware, cluster, values);
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        replaceCommonResources(quota, values.getJSONObject(RESOURCES));
        replaceCommonStorages(quota, values);

        // mysql参数
        JSONObject mysqlArgs = values.getJSONObject("mysqlArgs");
        if (StringUtils.isBlank(middleware.getPassword())) {
            middleware.setPassword(PasswordUtils.generateCommonPassword(10));
        }
        mysqlArgs.put("root_password", middleware.getPassword());
        if (StringUtils.isNotBlank(middleware.getCharSet())) {
            mysqlArgs.put("character_set_server", middleware.getCharSet());
        }
        if (middleware.getPort() != null) {
            mysqlArgs.put("server_port", middleware.getPort());
        }

        // 备份恢复的创建
        if (StringUtils.isNotEmpty(middleware.getBackupFileName())) {
            BackupStorageProvider backupStorageProvider = backupService.getStorageProvider(middleware);
            values.put("storageProvider", JSONObject.toJSON(backupStorageProvider));
        }
    }
}
```

##### 对于某个中间件独有的方法处理
如现在只有mysql有备份删除，则直接在`MysqlServiceImpl`里注入`MysqlOperator`，然后接口写在`MysqlOperator`里，实现写在MysqlOperatorImpl（如果是mysql通用的实现，可以写在`AbstractMysqlOperator`）
```java
public class MysqlServiceImpl extends AbstractMiddlewareService implements MysqlService {
    
    @Autowired
    private MysqlOperator mysqlOperator;

    @Override
    public List<MysqlBackupDto> listBackups(String clusterId, String namespace, String middlewareName) {
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        return mysqlOperator.listBackups(middleware);
    }


}
```
`MysqlOperator`里增加接口
```java
public interface MysqlOperator extends BaseOperator {

    List<MysqlBackupDto> listBackups(Middleware middleware);
}
```
`MysqlOperatorImpl`里实现接口
```java
@Operator(paramTypes4One = Middleware.class)
public class MysqlOperatorImpl extends AbstractMysqlOperator implements MysqlOperator {
    
    /**
     * 查询备份列表
     */
    @Override
    public List<MysqlBackupDto> listBackups(Middleware middleware) {

        // 获取Backup
        String name = getBackupName(middleware);
        List<Backup> backupList = backupService.listBackup(middleware.getClusterId(), middleware.getNamespace());
        backupList = backupList.stream().filter(backup -> backup.getName().contains(name)).collect(Collectors.toList());

        // 获取当前备份中的状态
        List<MysqlBackupDto> mysqlBackupDtoList = new ArrayList<>();

        backupList.forEach(backup -> {
            MysqlBackupDto mysqlBackupDto = new MysqlBackupDto();
            if (!"Complete".equals(backup.getPhase())) {
                mysqlBackupDto.setStatus(backup.getPhase());
                mysqlBackupDto.setBackupFileName("");
            } else {
                mysqlBackupDto.setStatus("Complete");
                mysqlBackupDto.setBackupFileName(backup.getBackupFileName());
            }
            mysqlBackupDto.setBackupName(backup.getName());
            mysqlBackupDto.setDate(DateUtils.parseUTCDate(backup.getBackupTime()));
            mysqlBackupDto.setPosition("minio(" + backup.getEndPoint() + "/" + backup.getBucketName() + ")");
            mysqlBackupDto.setType("all");
            mysqlBackupDtoList.add(mysqlBackupDto);
        });

        // 根据时间降序
        mysqlBackupDtoList.sort(
            (o1, o2) -> o1.getDate() == null ? -1 : o2.getDate() == null ? -1 : o2.getDate().compareTo(o1.getDate()));
        return mysqlBackupDtoList;
    }
}
```