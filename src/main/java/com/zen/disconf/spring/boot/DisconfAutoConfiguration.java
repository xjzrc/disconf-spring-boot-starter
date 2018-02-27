package com.zen.disconf.spring.boot;

import com.baidu.disconf.client.DisconfMgrBean;
import com.baidu.disconf.client.DisconfMgrBeanSecond;
import com.baidu.disconf.client.addons.properties.ReloadablePropertiesFactoryBean;
import com.baidu.disconf.client.addons.properties.ReloadingPropertyPlaceholderConfigurer;
import com.baidu.disconf.client.support.utils.ClassUtils;
import com.baidu.disconf.client.support.utils.StringUtil;
import com.zen.disconf.spring.boot.annotation.DisconfConfigAnnotation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * disconf自动配置
 *
 * @author xinjingziranchan@gmail.com
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class DisconfAutoConfiguration implements EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisconfAutoConfiguration.class);

    private static DisconfProperties disconfProperties = new DisconfProperties();

    private Environment environment;

    /**
     * 加载配置
     *
     * @param environment 配置环境
     */
    private void loadConfig(Environment environment) {
        Field[] fields = disconfProperties.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(DisconfConfigAnnotation.class)) {
                DisconfConfigAnnotation config = field.getAnnotation(DisconfConfigAnnotation.class);
                // 获取配置属性的值
                String value = environment.getProperty(DisconfProperties.DISCONF_PROPERTIES_PREFIX + "." + config.springBootName(), config.defaultValue());
                // 设置到系统环境变量中，给disClientConfig解析
                System.setProperty(config.disconfName(), value);
                try {
                    field.setAccessible(true);
                    ClassUtils.setFieldValeByType(field, disconfProperties, value);
                } catch (Exception e) {
                    LOGGER.error(String.format("invalid config: %s", config.springBootName()), e);
                }
            }
        }
    }

    /**
     * 设置扫描包路径
     * 初始化DisconfMgrBean
     *
     * @return DisconfMgrBean
     */
    @Bean(destroyMethod = "destroy")
    public DisconfMgrBean disconfMgrBean() {
        //加载配置
        loadConfig(environment);
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
    @Bean
    public ReloadablePropertiesFactoryBean disconfUnReloadablePropertiesFactoryBean() {
        ReloadablePropertiesFactoryBean factoryBean = new ReloadablePropertiesFactoryBean();
        factoryBean.setLocations(StringUtil.parseStringToStringList(disconfProperties.getUnReloadFiles(), ","));
        return factoryBean;
    }

    /**
     * 当配置文件改变，不会自动reload到系统
     *
     * @return PropertyPlaceholderConfigurer
     * @throws IOException IOException
     */
    @Bean
    public PropertyPlaceholderConfigurer disconfPropertyPlaceholderConfigurer() throws IOException {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        configurer.setProperties(disconfUnReloadablePropertiesFactoryBean().getObject());
        return configurer;
    }

    /**
     * 使用托管方式的disconf配置
     *
     * @return ReloadablePropertiesFactoryBean
     */
    @Bean
    public ReloadablePropertiesFactoryBean disconfReloadablePropertiesFactoryBean() {
        ReloadablePropertiesFactoryBean factoryBean = new ReloadablePropertiesFactoryBean();
        factoryBean.setLocations(StringUtil.parseStringToStringList(disconfProperties.getReloadFiles(), ","));
        return factoryBean;
    }

    /**
     * 当配置文件改变，会自动reload到系统
     *
     * @return ReloadingPropertyPlaceholderConfigurer
     * @throws IOException IOException
     */
    @Bean
    public ReloadingPropertyPlaceholderConfigurer disconfReloadingPropertyPlaceholderConfigurer() throws IOException {
        ReloadingPropertyPlaceholderConfigurer configurer = new ReloadingPropertyPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        configurer.setProperties(disconfReloadablePropertiesFactoryBean().getObject());
        return configurer;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
