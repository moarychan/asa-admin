package com.azure.spring.asa.component.admin.autoconfigure;


import com.azure.spring.asa.component.admin.autoconfigure.properties.AsaAdminProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@EnableAutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.cloud.azure.admin.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AsaAdminProperties.class)
@Import({ AsaAdminConfigurationImportSelector.class, AsaEntraIdSecurityConfiguration.class })
public class AsaAdminAutoConfiguration {

}
