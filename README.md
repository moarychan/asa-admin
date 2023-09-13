[![Java CI with Maven](https://github.com/fangjian0423/asa-admin/actions/workflows/maven.yml/badge.svg)](https://github.com/fangjian0423/asa-admin/actions/workflows/maven.yml)

# Introduction - Azure Spring Apps Admin

Spring Boot Admin Server is an application for managing and monitoring microservice applications.

This repository maintains a customized Spring Boot Admin Server - **Azure Spring Apps Admin**, which did some enhancements beyond community Spring Boot Admin.

# What's the benefit of Azure Spring Apps Admin

1. Support integration with Microsoft Entra ID (Original name: Azure AD) to secure your dashboard.
2. Support on-click Azure Spring Apps button for easy deployment without using multiple Azure commands.
3. Fully compatible with community Spring Boot Admin.

# Run Azure Spring Apps Admin on Azure Spring Apps

Azure Spring Apps Admin provides two usage methods. 
By default, the Spring Boot Admin application is not protected, 
and Microsoft Entra ID protection can also be enabled by switching `spring.cloud.azure.admin.entra-id.enabled`.

## Deploy unprotected components to Azure Spring Apps

To deploy an unprotected component, you only need an Azure subscription, and then you can run this Azure Spring Apps Admin with one click button:

<a href="https://yonghui-apps-dev-nubesgen.azuremicroservices.io/deploy.html?url=https://github.com/fangjian0423/asa-admin" data-linktype="external">
<img src="https://user-images.githubusercontent.com/58474919/236122963-8c0857bb-3822-4485-892a-445fa33f1612.png" alt="Deploy to Azure Spring Apps" width="200px" data-linktype="relative-path">
</a>

Once the deployment is complete, you will redirect the application endpoint, then you can access the Spring Boot Admin dashboard.

## Deploy the component integrated with Microsoft Entra ID to Azure Spring Apps

To integrate with Microsoft Entra ID and deploy Spring Boot Admin to Azure Spring Apps using the Azure Spring Apps button, you need below prerequisites:

- An Azure subscription
- Microsoft Entra ID switch to enable Azure Spring Apps Admin Microsoft Entra ID - Adding `SPRING_CLOUD_AZURE_ADMIN_ENTRAID_ENABLED` environment variable
- A Microsoft Entra ID administrative roles user
- Microsoft Entra ID registration application client id to enable Microsoft Entra ID - Adding `AAD_CLIENT_ID` environment variable
- Microsoft Entra ID registration application client secret to enable Microsoft Entra ID - Adding `AAD_CLIENT_SECRET` environment variable
- Microsoft Entra ID registration application tenant id to enable Microsoft Entra ID - Adding `AAD_TENANT_ID` environment variable

Then, you can run this Azure Spring Apps Admin with one click button:

<a href="https://yonghui-apps-dev-nubesgen.azuremicroservices.io/deploy.html?url=https://github.com/fangjian0423/asa-admin" data-linktype="external">
<img src="https://user-images.githubusercontent.com/58474919/236122963-8c0857bb-3822-4485-892a-445fa33f1612.png" alt="Deploy to Azure Spring Apps" width="200px" data-linktype="relative-path">
</a>

Once the deployment is complete, you will redirect the application endpoint, but you will not be able to log in successfully, which requires further configuration below.

Use the following steps to update configuration to integrate the newly generated FQDN:
- Update the redirect uri in the Microsoft Entra ID registration application, the format is union `application url`, `admin context path` and `/admin/login/oauth2/code/`, such as `https://<app-name>.xxx.xxx.azurecontainerapps.io/admin/login/oauth2/code/`.
- Update the `FULLY_QUALIFIED_DOMAIN_NAME` environment variable for Spring Boot Admin app, which does not include the Spring Boot Admin's context path, such as `https://<app-name>.xxx.xxx.azurecontainerapps.io`.

After the above steps are completed, refresh the Spring boot Admin login page, then to do OAuth2 login.

If you meet below error, please refer [Add a redirect URI](https://learn.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app#add-a-redirect-uri) to add a redirect url:

![](assets/aad-login.png)

After everything deploy successful, managing and monitoring your apps via Azure Spring Apps Admin.

![](assets/dashboard.png)
