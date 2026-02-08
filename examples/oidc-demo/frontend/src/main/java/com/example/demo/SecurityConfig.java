package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/flow/**", "/error", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(withDefaults())
                .csrf(csrf -> csrf.disable()); // Disable CSRF for demo simplicity (esp. for POST flows)

        return http.build();
    }
}
