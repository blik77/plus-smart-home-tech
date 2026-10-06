package ru.yandex.practicum.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false",
        "app.security.users[0].username=ivan",
        "app.security.users[0].password=ivan",
        "app.security.users[0].roles[0]=USER",
        "app.security.users[1].username=anna",
        "app.security.users[1].password=anna",
        "app.security.users[1].roles[0]=USER",
        "app.security.users[1].roles[1]=ADMIN",
        "spring.cloud.gateway.routes[0].id=test-products",
        "spring.cloud.gateway.routes[0].uri=forward:/test-backend",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/products/**",
        "spring.cloud.gateway.routes[1].id=test-orders",
        "spring.cloud.gateway.routes[1].uri=forward:/test-backend",
        "spring.cloud.gateway.routes[1].predicates[0]=Path=/api/orders/**"
    }
)
@AutoConfigureWebTestClient
@Import({
    GatewaySecurityConfigTest.TestBackendConfig.class,
    GatewaySecurityConfigTest.TestSecurityConfig.class
})
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void catalogGet_isPublic() {
        webTestClient.get().uri("/api/products")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void orderCreate_withoutCredentials_isUnauthorized() {
        webTestClient.post().uri("/api/orders")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void orderCreate_withUserCredentials_passesSecurity() {
        webTestClient.post().uri("/api/orders")
            .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void productWrite_withUserCredentials_isForbidden() {
        webTestClient.post().uri("/api/products")
            .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void productWrite_withAdminCredentials_passesSecurity() {
        webTestClient.post().uri("/api/products")
            .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void ordersList_withUserCredentials_isForbidden() {
        webTestClient.get().uri("/api/orders")
            .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void ordersList_withAdminCredentials_passesSecurity() {
        webTestClient.get().uri("/api/orders")
            .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void unknownRoute_withAdminCredentials_isForbidden() {
        webTestClient.get().uri("/api/unknown")
            .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void corsPreflight_isPublic() {
        webTestClient.options().uri("/api/orders")
            .exchange()
            .expectStatus().isOk();
    }

    private String basic(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route(path("/test-backend"), request -> ServerResponse.ok().build());
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Primary
        public PasswordEncoder testPasswordEncoder() {
            return NoOpPasswordEncoder.getInstance();
        }
    }
}