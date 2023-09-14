package com.azure.spring.asa.component.admin.autoconfigure.client;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadata;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.Map;

import static org.springframework.cloud.commons.util.IdUtils.getDefaultInstanceId;

/**
 * Enhanced implementation of {@link EurekaClientAutoConfiguration}, it's compatible with Azure Spring Apps Standard consumption and dedicated.
 */
@Configuration(proxyBeanMethods = false)
public class AsaEurekaClientConfiguration {

    private final ConfigurableEnvironment env;

    public AsaEurekaClientConfiguration(ConfigurableEnvironment env) {
        this.env = env;
    }

    @Bean
    public ManagementMetadataProvider serviceManagementMetadataProvider() {
        return new AsaDefaultManagementMetadataProvider();
    }

    private String getProperty(String property) {
        return this.env.containsProperty(property) ? this.env.getProperty(property) : "";
    }

    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils,
                                                             ManagementMetadataProvider managementMetadataProvider) {
        String hostname = getProperty("eureka.instance.hostname");
        boolean preferIpAddress = Boolean.parseBoolean(getProperty("eureka.instance.prefer-ip-address"));
        String ipAddress = getProperty("eureka.instance.ip-address");
        boolean isSecurePortEnabled = Boolean.parseBoolean(getProperty("eureka.instance.secure-port-enabled"));

        String serverContextPath = env.getProperty("server.servlet.context-path", "/");
        int serverPort = Integer.parseInt(env.getProperty("server.port", env.getProperty("port", "8080")));

        Integer managementPort = env.getProperty("management.server.port", Integer.class);

        String managementContextPath = env.getProperty("management.server.servlet.context-path");
        if (!StringUtils.hasText(managementContextPath)) {
            managementContextPath = env.getProperty("management.server.base-path");
        }

        Integer jmxPort = env.getProperty("com.sun.management.jmxremote.port", Integer.class);
        EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);

        instance.setNonSecurePort(serverPort);
        instance.setInstanceId(getDefaultInstanceId(env));
        instance.setPreferIpAddress(preferIpAddress);
        instance.setSecurePortEnabled(isSecurePortEnabled);
        if (StringUtils.hasText(ipAddress)) {
            instance.setIpAddress(ipAddress);
        }

        // It's wrong logic because when the secure port is enabled the secure port will be set to the default server port 8080.
//        if (isSecurePortEnabled) {
//            instance.setSecurePort(serverPort);
//        }

        if (StringUtils.hasText(hostname)) {
            instance.setHostname(hostname);
        }
        String statusPageUrlPath = getProperty("eureka.instance.status-page-url-path");
        String healthCheckUrlPath = getProperty("eureka.instance.health-check-url-path");

        if (StringUtils.hasText(statusPageUrlPath)) {
            instance.setStatusPageUrlPath(statusPageUrlPath);
        }
        if (StringUtils.hasText(healthCheckUrlPath)) {
            instance.setHealthCheckUrlPath(healthCheckUrlPath);
        }

        ManagementMetadata metadata = managementMetadataProvider.get(instance, serverPort, serverContextPath,
            managementContextPath, managementPort);

        if (metadata != null) {
            instance.setStatusPageUrl(metadata.getStatusPageUrl());
            instance.setHealthCheckUrl(metadata.getHealthCheckUrl());
            if (instance.isSecurePortEnabled()) {
                instance.setSecureHealthCheckUrl(metadata.getSecureHealthCheckUrl());
            }
            Map<String, String> metadataMap = instance.getMetadataMap();
            metadataMap.computeIfAbsent("management.port", k -> String.valueOf(metadata.getManagementPort()));
        }
        else {
            // without the metadata the status and health check URLs will not be set
            // and the status page and health check url paths will not include the
            // context path so set them here
            if (StringUtils.hasText(managementContextPath)) {
                instance.setHealthCheckUrlPath(managementContextPath + instance.getHealthCheckUrlPath());
                instance.setStatusPageUrlPath(managementContextPath + instance.getStatusPageUrlPath());
            }
        }

        setupJmxPort(instance, jmxPort);
        return instance;
    }

    private void setupJmxPort(EurekaInstanceConfigBean instance, Integer jmxPort) {
        Map<String, String> metadataMap = instance.getMetadataMap();
        if (metadataMap.get("jmx.port") == null && jmxPort != null) {
            metadataMap.put("jmx.port", String.valueOf(jmxPort));
        }
    }
}
