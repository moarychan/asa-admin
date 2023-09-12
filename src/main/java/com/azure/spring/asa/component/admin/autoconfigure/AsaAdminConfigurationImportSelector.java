package com.azure.spring.asa.component.admin.autoconfigure;

import com.azure.spring.asa.component.admin.autoconfigure.properties.AsaAdminProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Set;

/**
 * Exclude the Spring Security auto-configuration if the Azure Spring Apps Admin Entra ID is not enabled.
 */
public class AsaAdminConfigurationImportSelector extends AutoConfigurationImportSelector {

    private static final String PROPERTY_NAME_ASA_ADMIN = "spring.cloud.azure.admin";

    private static final Log logger = LogFactory.getLog(AsaAdminConfigurationImportSelector.class);

    @Override
    protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
        Set<String> exclusions = super.getExclusions(metadata, attributes);
        Environment environment = getEnvironment();
        if (environment instanceof ConfigurableEnvironment) {
            Binder binder = Binder.get(environment);
            AsaAdminProperties asaAdminProperties =
                binder.bind(PROPERTY_NAME_ASA_ADMIN, AsaAdminProperties.class).orElse(null);
            if (asaAdminProperties != null && !asaAdminProperties.getEntraId().isEnabled()) {
                logger.debug("Integration with Microsoft Entra ID is not used by default.");
                exclusions.add("com.azure.spring.cloud.autoconfigure.aad.AadAutoConfiguration");
                exclusions.add("com.azure.spring.cloud.autoconfigure.aad.AadAuthenticationFilterAutoConfiguration");
                exclusions.add("org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration");
                exclusions.add("org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration");
            }
        }
        return exclusions;
    }
}