package com.azure.spring.asa.component.admin.autoconfigure;

import com.azure.spring.asa.component.admin.web.LoginController;
import com.azure.spring.cloud.autoconfigure.aad.AadClientRegistrationRepository;
import com.azure.spring.cloud.autoconfigure.aad.AadOAuth2AuthorizationRequestResolver;
import com.azure.spring.cloud.autoconfigure.aad.implementation.jwt.AadJwtClientAuthenticationParametersConverter;
import com.azure.spring.cloud.autoconfigure.aad.implementation.oauth2.OAuth2ClientAuthenticationJwkResolver;
import com.azure.spring.cloud.autoconfigure.aad.implementation.webapp.AadOAuth2AuthorizationCodeGrantRequestEntityConverter;
import com.azure.spring.cloud.autoconfigure.aad.properties.AadAuthenticationProperties;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.ui.config.AdminServerUiProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

import javax.ws.rs.HttpMethod;

import static com.azure.spring.cloud.autoconfigure.aad.implementation.AadRestTemplateCreator.createOAuth2AccessTokenResponseClientRestTemplate;

@Configuration(proxyBeanMethods = false)
public class SecurityAutoConfiguration {

    @Autowired
    protected ClientRegistrationRepository repo;


    /**
     * restTemplateBuilder bean used to create RestTemplate for Azure AD related http request.
     */
    @Autowired
    protected RestTemplateBuilder restTemplateBuilder;

    /**
     * OIDC user service.
     */
    @Autowired
    protected OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

    /**
     * AAD authentication properties
     */
    @Autowired
    protected AadAuthenticationProperties properties;

    @Autowired
    protected ObjectProvider<OAuth2ClientAuthenticationJwkResolver> jwkResolvers;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
//        return (web) -> web.ignoring().antMatchers("/mgmt3/actuator/health");
        return (web) -> web.debug(true);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AdminServerProperties adminServer, ServerProperties serverProperties) throws Exception {
        // @formatter:off
        http.oauth2Login()
                .authorizationEndpoint()
                    .authorizationRequestResolver(requestResolver(adminServer))
                    .and()
                .tokenEndpoint()
                    .accessTokenResponseClient(accessTokenResponseClient())
                    .and()
                .userInfoEndpoint()
                    .oidcUserService(oidcUserService)
                    .and()
                .and()
            .logout()
                .logoutSuccessHandler(oidcLogoutSuccessHandler());
        // @formatter:off

        http.authorizeRequests(
                (authorizeRequests) ->
                    authorizeRequests.antMatchers(adminServer.path("/assets/**")).permitAll()
                                                        .antMatchers("/actuator/info", "/actuator/health").permitAll()
                                                        .antMatchers(adminServer.path("/login_oauth2")).permitAll().anyRequest().authenticated()
            ).oauth2Login(
                login ->
                    login.defaultSuccessUrl(adminServer.path("/"), true)
                         .authorizationEndpoint(authorization -> authorization.baseUri(adminServer.path("/oauth2/authorization")))
                         .loginPage(adminServer.path("/login_oauth2"))
            ).logout((logout) -> logout.logoutUrl(adminServer.path("/logout")).logoutSuccessUrl(adminServer.path("/login_oauth2") + "?logoutSuccess=1"))
            .csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .ignoringRequestMatchers(
                                    new AntPathRequestMatcher(adminServer.path("/instances"),
                                        HttpMethod.POST.toString()),
                                    new AntPathRequestMatcher(adminServer.path("/instances/*"),
                                        HttpMethod.DELETE.toString()),
                                    new AntPathRequestMatcher(adminServer.path("/actuator/**"))
                                ));
        return http.build();
    }

    /**
     * Gets the OIDC logout success handler.
     *
     * @return the OIDC logout success handler
     */
    protected LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
            new OidcClientInitiatedLogoutSuccessHandler(this.repo);
        String uri = this.properties.getPostLogoutRedirectUri();
        if (StringUtils.hasText(uri)) {
            oidcLogoutSuccessHandler.setPostLogoutRedirectUri(uri);
        }
        return oidcLogoutSuccessHandler;
    }

    /**
     * Gets the access token response client.
     *
     * @return the access token response client
     */
    protected OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient result = new DefaultAuthorizationCodeTokenResponseClient();
        result.setRestOperations(createOAuth2AccessTokenResponseClientRestTemplate(restTemplateBuilder));
        if (repo instanceof AadClientRegistrationRepository) {
            AadOAuth2AuthorizationCodeGrantRequestEntityConverter converter =
                new AadOAuth2AuthorizationCodeGrantRequestEntityConverter(
                    ((AadClientRegistrationRepository) repo).getAzureClientAccessTokenScopes());
            OAuth2ClientAuthenticationJwkResolver jwkResolver = jwkResolvers.getIfUnique();
            if (jwkResolver != null) {
                converter.addParametersConverter(new AadJwtClientAuthenticationParametersConverter<>(jwkResolver::resolve));
            }
            result.setRequestEntityConverter(converter);
        }
        return result;
    }

    /**
     * Gets the request resolver.
     *
     * @return the request resolver
     */
    protected OAuth2AuthorizationRequestResolver requestResolver(AdminServerProperties adminServer) {
        return new AadOAuth2AuthorizationRequestResolver(adminServer.path("/oauth2/authorization"), this.repo, properties);
    }
    @Bean
    public LoginController loginController(AdminServerUiProperties adminUi, AdminServerProperties adminServer,
                                           ApplicationContext applicationContext,
                                           ClientRegistrationRepository clientRegistrationRepository,
                                           ServerProperties serverProperties) {
        return new LoginController(adminUi, adminServer, applicationContext, clientRegistrationRepository, serverProperties);
    }

}
