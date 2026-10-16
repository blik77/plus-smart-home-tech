package ru.yandex.practicum.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .cors(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**"
                ).permitAll()
                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/inventory/**").permitAll()
                .pathMatchers(HttpMethod.POST,
                    "/api/products/**", "/api/categories/**", "/api/inventory/**")
                .hasRole("ADMIN")
                .pathMatchers(HttpMethod.PUT,
                    "/api/products/**", "/api/categories/**", "/api/inventory/**")
                .hasRole("ADMIN")
                .pathMatchers(HttpMethod.PATCH,
                    "/api/products/**", "/api/categories/**", "/api/inventory/**")
                .hasRole("ADMIN")
                .pathMatchers(HttpMethod.DELETE,
                    "/api/products/**", "/api/categories/**", "/api/inventory/**")
                .hasRole("ADMIN")
                .pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                .pathMatchers(HttpMethod.GET, "/api/orders/by-email").hasRole("USER")
                .pathMatchers(HttpMethod.GET, "/api/orders/**").hasRole("USER")
                .pathMatchers(HttpMethod.POST, "/api/orders/**").hasRole("USER")
                .anyExchange().denyAll()
            )
            .build();
    }
}