package ru.yandex.practicum.apigateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityUserConfig {

    private final SecurityProperties securityProperties;

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        List<UserDetails> users = securityProperties.getUsers().stream()
            .map(this::toUserDetails)
            .toList();

        return new MapReactiveUserDetailsService(users);
    }


    private UserDetails toUserDetails(SecurityProperties.UserConfig userConfig) {
        return User.builder()
            .username(userConfig.getUsername())
            .password(userConfig.getPassword())
            .roles(userConfig.getRoles().toArray(String[]::new))
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}