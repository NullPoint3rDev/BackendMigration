package org.alloy.config;

import org.alloy.security.JwtAuthenticationFilter;
import org.alloy.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://*",
                "https://*",
                "http://localhost:*",
                "http://192.168.*:*",
                "http://95.172.*:*",
                "http://5.227.*:*",
                "http://alloynn.keenetic.name:*",
                "http://89.109.8.59"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoint are available for all type of users (login page, swagger docs, websocket for welding machines, health-check)
                        .requestMatchers("/auth/**", "/login/**", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**", "/health").permitAll()
                        // Actuator на отдельном management-порту (9100), не публикуется наружу — доступен только Prometheus внутри docker-сети
                        .requestMatchers("/actuator/**").permitAll()

                        // User accounts — детализация в контроллерах по PERMISSION_*
                        .requestMatchers(HttpMethod.GET, "/user-accounts/current").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/user-accounts/profile").authenticated()
                        .requestMatchers(HttpMethod.POST, "/user-accounts/photo").authenticated()
                        .requestMatchers(HttpMethod.GET, "/user-accounts/photo/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/user-accounts/username/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/user-accounts/email/**").authenticated()
                        .requestMatchers("/user-accounts/**").authenticated()
                        .requestMatchers("/user-roles/**").authenticated()
                        .requestMatchers("/user-permissions/**").authenticated()
                        .requestMatchers("/user-role-permissions/**").authenticated()
                        .requestMatchers("/user-permission-grants/**").authenticated()

                        // Reports, equipment, orgs, welders, notifications — проверка по правам в контроллерах
                        .requestMatchers("/reports/**").authenticated()
                        .requestMatchers("/automated-reports/**").authenticated()
                        .requestMatchers("/devices/**").authenticated()
                        .requestMatchers("/welding-devices/**").authenticated()
                        .requestMatchers("/welding-machines/**").authenticated()
                        .requestMatchers("/mac-address-registry/**").authenticated()
                        .requestMatchers("/device-test/**").authenticated()
                        .requestMatchers("/employees/**").authenticated()
                        .requestMatchers("/welders/**").authenticated()
                        .requestMatchers("/organizations/**").authenticated()
                        .requestMatchers("/organization-units/**").authenticated()
                        .requestMatchers("/system-settings/**").authenticated()
                        .requestMatchers("/email-templates/**").authenticated()
                        .requestMatchers("/email-test/**").authenticated()
                        .requestMatchers("/notifications/**").authenticated()
                        .requestMatchers("/notification-templates/**").authenticated()
                        .requestMatchers("/library-documents/**").authenticated()
                        .requestMatchers("/certifications/**").authenticated()
                        .requestMatchers("/maintenance/**").authenticated()
                        .requestMatchers("/welding-procedure-specifications/**").authenticated()

                        // The rest of the endpoints are available for all authorized users
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
