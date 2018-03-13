 disconf-spring-boot-starter
===================================
 disconf-spring-boot-starter
让你可以使用spring-boot的方式开发依赖disconf的程序

只需要关心disconfi的配置文件和配置项，省略了编写xml的麻烦
*****

### 使用步骤(示例:[「spring-boot-starter-demo」](https://github.com/xjzrc/spring-boot-starter-demo))

* 在`spring boot`项目的`pom.xml`中添加以下依赖：

根据实际情况依赖最新版本
```xml
<dependency>
    <groupId>com.github.xjzrc.spring.boot</groupId>
    <artifactId>disconf-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```
<br/>
* 在application.yml添加disconf的相关配置信息<br/>
特别注意：配置属性名规则只支持如下样例模式，否则无法解析。如：scan-package不能写成scanPackage
<br/>具体样例配置如下:

```yml
spring:
  aop:
    #使你的项目支持 cglib的aop
    proxy-target-class: true
  disconf:
    #disconf包扫描路径
    scan-package: com.zen.spring.boot.demo.disconf
    #不需要自动reload的配置文件(必须配置,没有留空)
    un-reload-files: myserver.properties
    #需要自动reload的配置文件(必须配置,没有留空)
    reload-files: autoconfig.properties,autoconfig2.properties,myserver_slave.properties,testJson.json,testXml2.xml
    #是否使用远程配置文件，true(默认)会从远程获取配置 false则直接获取本地配置
    enable-remote-conf: true
    #配置服务器的 HOST(必填)，用逗号分隔  127.0.0.1:8000,127.0.0.1:8000
    conf-server-host: 127.0.0.1
    #APP 请采用 产品线_服务名 格式
    app: disconf-demo
    #环境
    env: rd
    #版本, 请采用 X_X_X_X 格式,注意要用单引号，不然Springboot将会解析成1000
    version: '1_0_0_0'
    #忽略哪些分布式配置，用逗号分隔
    ignore:
    #调试模式。调试模式下，ZK超时或断开连接后不会重新连接（常用于client单步debug）。非调试模式下，ZK超时或断开连接会自动重新连接。
    debug: false
    #获取远程配置 重试次数，默认是3次
    conf-server-url-retry-times: 1
    #获取远程配置 重试时休眠时间，默认是5秒
    conf-server-url-retry-sleep-seconds: 1
    #用户定义的下载文件夹, 远程文件下载后会放在这里。注意，此文件夹必须有有权限，否则无法下载到这里
    user-define-download-dir: /app/spring-boot/disconf/disconf-spring-boot-starter-demo
    #下载的文件会被迁移到classpath根路径下，强烈建议将此选项置为 true(默认是true)
    enable-local-download-dir-in-class-path: true
```

<br/>
* 不需要动态推送配置写法<br/>
  * 添加disconf配置文件名到application.properties中的disconf.files配置项，多个文件用逗号分隔
  
  * 直接用@Value注释引用变量
  
  ```java
      @Value("${database.driverClassName}")
      private String driverClassName;
  ```
 
* 需要动态推送配置写法<br/>

```java
     @Service
     @Scope("singleton")
     @DisconfFile(filename = "app-conf.properties")
     public class AppConfig {
         /**
          * 配置项属性
          */
         private String property;
         
          @DisconfFileItem(name = "config", associateField = "property")
           public void setProperty(String property) {
               this.property = property;
           }
     }
  ```
    
* springboot原有starter中需要依赖disconf配置怎么办?比如redis等 
  * 直接在disconf上面配置springboot starter中要求的配置项即可
  ```java
  原理
  应用启动时，disconf starter
  从disconf服务器上拉取配置文件，下载到本地，并且解析文件中的变量注入到spring环境Environment中, 供依赖的springboot starter使用
  springboot在初始化starter中的配置类时，会循环读取Environment中包含的各个properties，有符合配置类的变量，就注入到相应配置类
  ```



