package com.azure.spring.asa.component.admin.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Azure Spring Apps Admin.
 */
@ConfigurationProperties(prefix = AsaAdminProperties.PREFIX)
public class AsaAdminProperties {
    public static final String PREFIX = "spring.cloud.azure.admin";

    private boolean enabled = true;
    private String fullyQualifiedDomainName;
    private final EntraIDProvider entraId = new EntraIDProvider();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFullyQualifiedDomainName() {
        return fullyQualifiedDomainName;
    }

    public void setFullyQualifiedDomainName(String fullyQualifiedDomainName) {
        this.fullyQualifiedDomainName = fullyQualifiedDomainName;
    }

    public EntraIDProvider getEntraId() {
        return entraId;
    }

    /**
     * Properties dedicated to improve the integration with Microsoft Entra ID(Azure Active Directory).
     */
    public static class EntraIDProvider {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
