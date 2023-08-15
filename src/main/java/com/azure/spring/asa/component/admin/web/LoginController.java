package com.azure.spring.asa.component.admin.web;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.notify.filter.web.NotificationFilterController;
import de.codecentric.boot.admin.server.ui.config.AdminServerUiProperties;
import de.codecentric.boot.admin.server.ui.extensions.UiRoutesScanner;
import de.codecentric.boot.admin.server.ui.web.UiController;
import de.codecentric.boot.admin.server.web.AdminController;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AdminController
public class LoginController {

    private static String authorizationRequestBaseUri = "oauth2/authorization";

    private final AdminServerUiProperties adminUi;

    private final AdminServerProperties adminServer;

    private final ApplicationContext applicationContext;

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final ServerProperties serverProperties;

    public LoginController(AdminServerUiProperties adminUi, AdminServerProperties adminServer, ApplicationContext applicationContext,
                           ClientRegistrationRepository clientRegistrationRepository, ServerProperties serverProperties) {
        this.adminUi = adminUi;
        this.adminServer = adminServer;
        this.applicationContext = applicationContext;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.serverProperties = serverProperties;
    }


    @ModelAttribute(value = "baseUrl", binding = false)
    public String getBaseUrl() {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance();
        String publicUrl = (this.adminUi.getPublicUrl() != null) ? this.adminUi.getPublicUrl()
            : this.adminServer.getContextPath();
        UriComponents publicComponents = UriComponentsBuilder.fromUriString(publicUrl).build();
        if (publicComponents.getScheme() != null) {
            uriBuilder.scheme(publicComponents.getScheme());
        }
        if (publicComponents.getHost() != null) {
            uriBuilder.host(publicComponents.getHost());
        }
        if (publicComponents.getPort() != -1) {
            uriBuilder.port(publicComponents.getPort());
        }
        if (publicComponents.getPath() != null) {
            uriBuilder.path(publicComponents.getPath());
        }
        String baseUrl = serverProperties.getServlet().getContextPath() == null ?
            uriBuilder.path("/").toUriString() :
            serverProperties.getServlet().getContextPath() + uriBuilder.path("/").toUriString();

        return baseUrl;
    }

    @ModelAttribute(value = "uiSettings", binding = false)
    public UiController.Settings getUiSettings() throws IOException {
        List<String> extensionRoutes = new UiRoutesScanner(this.applicationContext)
            .scan(this.adminUi.getExtensionResourceLocations());
        List<String> routes = Stream.concat(Arrays.asList("/about/**", "/applications/**", "/instances/**",
                                        "/journal/**", "/wallboard/**", "/external/**").stream(), extensionRoutes.stream())
                                    .collect(Collectors.toList());
        UiController.Settings uiSettings =
            UiController.Settings.builder().brand(this.adminUi.getBrand()).title(this.adminUi.getTitle())
                                 .loginIcon(this.adminUi.getLoginIcon()).favicon(this.adminUi.getFavicon())
                                 .faviconDanger(this.adminUi.getFaviconDanger())
                                 .notificationFilterEnabled(
                                     !this.applicationContext.getBeansOfType(NotificationFilterController.class).isEmpty())
                                 .routes(routes).rememberMeEnabled(this.adminUi.isRememberMeEnabled())
                                 .availableLanguages(this.adminUi.getAvailableLanguages()).externalViews(this.adminUi.getExternalViews())
                                 .pollTimer(this.adminUi.getPollTimer()).viewSettings(this.adminUi.getViewSettings()).build();
        return uiSettings;
    }

    @ModelAttribute(value = "publicUrl", binding = false)
    public String publicUrl() throws IOException {
        String publicUrl = (this.adminUi.getPublicUrl() != null) ? this.adminUi.getPublicUrl()
            : this.adminServer.getContextPath();
        return publicUrl;
    }

    @ModelAttribute(value = "oAuthClientList", binding = false)
    public List<OAuthClient> oAuthClientList() throws IOException {
        Iterable<ClientRegistration> clientRegistrations = null;
        ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository).as(Iterable.class);
        if (type != ResolvableType.NONE &&
            ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
            clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
        }
        String baseUrl = getBaseUrl();
        List<OAuthClient> oAuthClientList = new ArrayList<>();
        clientRegistrations.forEach(registration ->
            oAuthClientList.add(
                new OAuthClient(registration.getClientName(),
                    baseUrl + authorizationRequestBaseUri + "/" + registration.getRegistrationId()))
        );
        return oAuthClientList;
    }

    @ModelAttribute(value = "logoutSuccess", binding = false)
    public String logoutSuccess(HttpServletRequest request) {
        return request.getParameter("logoutSuccess");
    }

    @GetMapping(path = "/login_oauth2", produces = MediaType.TEXT_HTML_VALUE)
    public String loginPage(HttpServletRequest request) throws IOException {
        return "login_oauth2";
    }

    public static class OAuthClient {
        private String clientName;
        private String redirectUrl;

        public OAuthClient(String clientName, String redirectUrl) {
            this.clientName = clientName;
            this.redirectUrl = redirectUrl;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public void setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }
    }

}
