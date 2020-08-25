package com.github.lazyboyl.socket.integrate;

import com.github.lazyboyl.socket.constant.SocketProperties;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author linzf
 * @since 2020/3/12
 * 类描述：
 */
public class SocketScannerRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private ResourceLoader resourceLoader;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableSocketServer.class.getName()));
        ClassPathSocketSecurityScanner scanner = new ClassPathSocketSecurityScanner(registry);
        // this check is needed in Spring 3.1
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
        }

        List<String> basePackages = new ArrayList<String>();
        for (String pkg : annoAttrs.getStringArray("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        ConfigurableEnvironment c = (ConfigurableEnvironment) environment;
        MutablePropertySources m = c.getPropertySources();
        Properties p = new Properties();
        p.put("socket.scan.package", annoAttrs.getStringArray("socketScanPackage"));
        for (SocketProperties wsp : SocketProperties.values()) {
            String val = environment.getProperty(wsp.getKey());
            if (val == null || "".equals(val)) {
                p.put(wsp.getKey(), wsp.getDefaultValue());
            }
        }
        m.addFirst(new PropertiesPropertySource("defaultProperties", p));
        scanner.doScan(StringUtils.toStringArray(basePackages));
    }
}
