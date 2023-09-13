package com.azure.spring.asa.component.admin.autoconfigure;

import com.azure.spring.asa.component.admin.autoconfigure.properties.AsaAdminProperties;
import com.azure.spring.asa.component.admin.web.LoginController;
import com.azure.spring.cloud.autoconfigure.aad.AadClientRegistrationRepository;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.ui.config.AdminServerUiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static com.azure.spring.cloud.autoconfigure.aad.implementation.AadRestTemplateCreator.createOAuth2AccessTokenResponseClientRestTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.cloud.azure.admin.entra-id.enabled", havingValue = "true")
public class AsaEntraIdSecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AdminServerProperties adminServerProperties,
                                                   ClientRegistrationRepository repo,
                                                   AsaAdminProperties asaAdminProperties,
                                                   RestTemplateBuilder restTemplateBuilder) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
            new OidcClientInitiatedLogoutSuccessHandler(repo);
        StringBuilder fqdn = new StringBuilder(asaAdminProperties.getFullyQualifiedDomainName());
        if (fqdn.charAt(fqdn.length() - 1) == '/') {
            fqdn.deleteCharAt(fqdn.length() - 1);
        }
        logoutSuccessHandler.setPostLogoutRedirectUri(fqdn + adminServerProperties.path("/login_oauth2") + "?logoutSuccess=1");
        http.authorizeRequests(
                (authorizeRequests) ->
                    authorizeRequests.antMatchers(adminServerProperties.path("/assets/**")).permitAll()
                                     .antMatchers("/actuator/info", "/actuator/health").permitAll()
                                     .antMatchers(adminServerProperties.path("/login_oauth2")).permitAll()
                                     .anyRequest().authenticated()
            ).oauth2Login(
                login ->
                    login.defaultSuccessUrl(adminServerProperties.path("/"), true)
                         .authorizationEndpoint(authorization -> authorization.baseUri(adminServerProperties.path("/oauth2/authorization")))
                         .loginPage(adminServerProperties.path("/login_oauth2"))
//                         .loginProcessingUrl(adminServerProperties.path("/login/oauth2/code/"))
                         .tokenEndpoint().accessTokenResponseClient(accessTokenResponseClient(repo, restTemplateBuilder))
            ).logout((logout) -> logout.logoutUrl(adminServerProperties.path("/logout")).logoutSuccessHandler(logoutSuccessHandler))
            .csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .ignoringRequestMatchers(
                                    new AntPathRequestMatcher(adminServerProperties.path("/instances"),
                                        HttpMethod.POST.toString()),
                                    new AntPathRequestMatcher(adminServerProperties.path("/instances/*"),
                                        HttpMethod.DELETE.toString()),
                                    new AntPathRequestMatcher(adminServerProperties.path("/actuator/**"))
                                ));
        return http.build();
    }

    protected OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient(ClientRegistrationRepository repo,
                                                                                                             RestTemplateBuilder restTemplateBuilder) {
        DefaultAuthorizationCodeTokenResponseClient result = new DefaultAuthorizationCodeTokenResponseClient();
        result.setRestOperations(createOAuth2AccessTokenResponseClientRestTemplate(restTemplateBuilder));
        if (repo instanceof AadClientRegistrationRepository) {
            OAuth2AuthorizationCodeGrantRequestEntityConverter converter =
                new OAuth2AuthorizationCodeGrantRequestEntityConverter();
            result.setRequestEntityConverter(converter);
        }
        return result;
    }

    @Bean
    public LoginController loginController(AdminServerUiProperties adminUi, AdminServerProperties adminServer,
                                           ApplicationContext applicationContext,
                                           ClientRegistrationRepository clientRegistrationRepository,
                                           ServerProperties serverProperties) {
        return new LoginController(adminUi, adminServer, applicationContext, clientRegistrationRepository, serverProperties);
    }

}
