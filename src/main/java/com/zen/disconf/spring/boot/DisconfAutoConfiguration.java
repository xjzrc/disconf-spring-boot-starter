package com.zen.disconf.spring.boot;

import com.baidu.disconf.client.DisconfMgrBean;
import com.baidu.disconf.client.DisconfMgrBeanSecond;
import com.baidu.disconf.client.addons.properties.ReloadablePropertiesFactoryBean;
import com.baidu.disconf.client.addons.properties.ReloadingPropertyPlaceholderConfigurer;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.support.utils.ClassUtils;
import com.baidu.disconf.client.support.utils.StringUtil;
import com.zen.disconf.spring.boot.annotation.DisconfConfigAnnotation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * disconf自动配置
 *
 * @author xinjingziranchan@gmail.com
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(DisconfMgrBean.class)
public class DisconfAutoConfiguration implements EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisconfAutoConfiguration.class);

    private DisconfProperties disconfProperties;

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
        this.disconfProperties = new DisconfProperties(this.environment);
        //加载Disconf 系统自带的配置
        loadDisClientSysConfig();
    }

    /**
     * 加载Disconf 系统自带的配置
     */
    private void loadDisClientSysConfig() {
        //加载Disconf 系统自带的配置
        DisClientSysConfig disClientSysConfig = DisClientSysConfig.getInstance();
        try {
            disClientSysConfig.loadConfig(null);
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        }
        //将系统下载目录替换成用户配置的下载目录
        disClientSysConfig.LOCAL_DOWNLOAD_DIR = disconfProperties.getUserDefineDownloadDir();
    }

    /**
     * 设置扫描包路径
     * 初始化DisconfMgrBean
     *
     * @return DisconfMgrBean
     */
    @Bean(destroyMethod = "destroy")
    public DisconfMgrBean disconfMgrBean() {
        DisconfMgrBean disconfMgrBean = new DisconfMgrBean();
        if (StringUtils.isBlank(disconfProperties.getScanPackage())) {
            LOGGER.error("disconf scan package is null!, please set the value in application.properties. (spring.disconf.scan-package=com.zen)");
            throw new RuntimeException("disconf scan package is null!, please set the value in application.properties. (spring.disconf.scan-package=com.zen)");
        }
        disconfMgrBean.setScanPackage(disconfProperties.getScanPackage());
        return disconfMgrBean;
    }

    /**
     * 二次扫描注入属性
     *
     * @return DisconfMgrBeanSecond
     */
    @Bean(initMethod = "init", destroyMethod = "destroy")
    public DisconfMgrBeanSecond disconfMgrBeanSecond() {
        return new DisconfMgrBeanSecond();
    }

    /**
     * 使用托管方式的disconf配置
     *
     * @return ReloadablePropertiesFactoryBean
     */
    @Bean("disconfUnReloadablePropertiesFactoryBean")
    @ConditionalOnExpression("'${spring.disconf.un-reload-files}'.length() > 0")
    public ReloadablePropertiesFactoryBean disconfUnReloadablePropertiesFactoryBean() {
        ReloadablePropertiesFactoryBean factoryBean = new ReloadablePropertiesFactoryBean();
        factoryBean.setLocations(getFileNames(disconfProperties.getUnReloadFiles()));
        return factoryBean;
    }


    /**
     * 当配置文件改变，不会自动reload到系统
     *
     * @param factoryBean 不会自动reload配置工厂
     * @return PropertyPlaceholderConfigurer
     * @throws IOException IOException
     */
    @Bean
    @ConditionalOnBean(name = "disconfUnReloadablePropertiesFactoryBean")
    public PropertyPlaceholderConfigurer disconfPropertyPlaceholderConfigurer(
            @Qualifier("disconfUnReloadablePropertiesFactoryBean") ReloadablePropertiesFactoryBean factoryBean) throws IOException {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setOrder(1);
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        configurer.setProperties(factoryBean.getObject());
        addPropertiesPropertySource("disconfUnReloadableProperties", factoryBean.getObject());
        return configurer;
    }

    /**
     * 使用托管方式的disconf配置
     *
     * @return ReloadablePropertiesFactoryBean
     */
    @Bean("disconfReloadablePropertiesFactoryBean")
    @ConditionalOnExpression("'${spring.disconf.reload-files}'.length() > 0")
    public ReloadablePropertiesFactoryBean disconfReloadablePropertiesFactoryBean() {
        ReloadablePropertiesFactoryBean factoryBean = new ReloadablePropertiesFactoryBean();
        factoryBean.setLocations(getFileNames(disconfProperties.getReloadFiles()));
        return factoryBean;
    }

    /**
     * 当配置文件改变，会自动reload到系统
     *
     * @param factoryBean 会自动reload配置工厂
     * @return ReloadingPropertyPlaceholderConfigurer
     * @throws IOException IOException
     */
    @Bean
    @ConditionalOnBean(name = "disconfReloadablePropertiesFactoryBean")
    public ReloadingPropertyPlaceholderConfigurer disconfReloadingPropertyPlaceholderConfigurer(
            @Qualifier("disconfReloadablePropertiesFactoryBean") ReloadablePropertiesFactoryBean factoryBean) throws IOException {
        ReloadingPropertyPlaceholderConfigurer configurer = new ReloadingPropertyPlaceholderConfigurer();
        configurer.setOrder(1);
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        configurer.setProperties(factoryBean.getObject());
        addPropertiesPropertySource("disconfReloadableProperties", factoryBean.getObject());
        return configurer;
    }

    /**
     * 获取文件列表集合
     *
     * @param files 文件
     * @return List<String>
     */
    private List<String> getFileNames(String files) {
        List<String> fileList = StringUtil.parseStringToStringList(files, ",");
        if (disconfProperties.isEnableLocalDownloadDirInClassPath()) {
            return fileList;
        }
        List<String> newFileList = new ArrayList<>(fileList.size());
        for (String file : fileList) {
            newFileList.add("file:" + disconfProperties.getUserDefineDownloadDir() + "/" + file);
        }
        return newFileList;
    }

    /**
     * 增加环境变量配置
     *
     * @param name   配置文件名
     * @param source 文件源
     */
    private void addPropertiesPropertySource(String name, Properties source) {
        PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource(name, source);
        environment.getPropertySources().addLast(propertiesPropertySource);
    }

}
