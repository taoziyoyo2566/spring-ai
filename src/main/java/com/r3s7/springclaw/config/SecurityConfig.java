package com.r3s7.springclaw.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * 安全配置：API 需要 HTTP Basic 认证，首页和健康检查端点放行。
 * 用户名密码通过 springclaw.security.username/password 或环境变量配置。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${springclaw.security.username:springclaw}")
    private String apiUsername;

    @Value("${springclaw.security.password:dev_secret}")
    private String apiPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }

    @Bean
    public UserDetailsService inMemoryUserDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.builder()
                .username(apiUsername)
                .password(encoder.encode(apiPassword))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(List.of(user));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
