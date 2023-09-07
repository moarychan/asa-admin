package com.azure.spring.asa.component.admin.autoconfigure;

import com.azure.spring.asa.component.admin.web.LoginController;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.ui.config.AdminServerUiProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration(proxyBeanMethods = false)
public class SecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AdminServerProperties adminServer,
                                                   ClientRegistrationRepository repo,
                                                   @Value("${fully_qualified_domain_name}") String fqdn) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
            new OidcClientInitiatedLogoutSuccessHandler(repo);
        StringBuilder contextPath = new StringBuilder(fqdn);
        if (contextPath.charAt(contextPath.length() - 1) == '/') {
            contextPath.deleteCharAt(contextPath.length() - 1);
        }
        logoutSuccessHandler.setPostLogoutRedirectUri(contextPath + adminServer.path("/login_oauth2") + "?logoutSuccess=1");
        http.authorizeRequests(
                (authorizeRequests) ->
                    authorizeRequests.requestMatchers(adminServer.path("/assets/**")).permitAll()
                                     .requestMatchers("/actuator/info", "/actuator/health").permitAll()
                                     .requestMatchers(adminServer.path("/login_oauth2")).permitAll()
                                     .anyRequest().authenticated()
            ).oauth2Login(
                login ->
                    login.defaultSuccessUrl(adminServer.path("/"), true)
                         .authorizationEndpoint(authorization -> authorization.baseUri(adminServer.path("/oauth2/authorization")))
                         .loginPage(adminServer.path("/login_oauth2"))
            ).logout((logout) -> logout.logoutUrl(adminServer.path("/logout"))
                                       .logoutSuccessHandler(logoutSuccessHandler))
            .csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                                .ignoringRequestMatchers(
                                    new AntPathRequestMatcher(adminServer.path("/instances"),
                                        HttpMethod.POST.toString()),
                                    new AntPathRequestMatcher(adminServer.path("/instances/*"),
                                        HttpMethod.DELETE.toString()),
                                    new AntPathRequestMatcher(adminServer.path("/actuator/**"))
                                ));
        return http.build();
    }

    @Bean
    public LoginController loginController(AdminServerUiProperties adminUi, AdminServerProperties adminServer,
                                           ApplicationContext applicationContext,
                                           ClientRegistrationRepository clientRegistrationRepository,
                                           ServerProperties serverProperties) {
        return new LoginController(adminUi, adminServer, applicationContext, clientRegistrationRepository, serverProperties);
    }

}
