package org.alloy.config;

import org.alloy.security.JwtAuthenticationFilter;
import org.alloy.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
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


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        System.out.println("SecurityConfig: configure(HttpSecurity) called!");
        http
                .cors().and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // Public endpoint are available for all type of users (login page, swagger docs, websocket for welding machines, health-check)
                .antMatchers("/auth/**", "/login/**", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**", "/health").permitAll()

                // User accounts — детализация в контроллерах по PERMISSION_*
                .antMatchers(HttpMethod.GET, "/user-accounts/current").authenticated()
                .antMatchers(HttpMethod.PUT, "/user-accounts/profile").authenticated()
                .antMatchers(HttpMethod.POST, "/user-accounts/photo").authenticated()
                .antMatchers(HttpMethod.GET, "/user-accounts/photo/**").authenticated()
                .antMatchers(HttpMethod.GET, "/user-accounts/username/**").authenticated()
                .antMatchers(HttpMethod.GET, "/user-accounts/email/**").authenticated()
                .antMatchers("/user-accounts/**").authenticated()
                .antMatchers("/user-roles/**").authenticated()
                .antMatchers("/user-permissions/**").authenticated()
                .antMatchers("/user-role-permissions/**").authenticated()
                .antMatchers("/user-permission-grants/**").authenticated()

                // Reports, equipment, orgs, welders, notifications — проверка по правам в контроллерах
                .antMatchers("/reports/**").authenticated()
                .antMatchers("/automated-reports/**").authenticated()
                .antMatchers("/devices/**").authenticated()
                .antMatchers("/welding-devices/**").authenticated()
                .antMatchers("/welding-machines/**").authenticated()
                .antMatchers("/device-test/**").authenticated()
                .antMatchers("/employees/**").authenticated()
                .antMatchers("/welders/**").authenticated()
                .antMatchers("/organizations/**").authenticated()
                .antMatchers("/organization-units/**").authenticated()
                .antMatchers("/system-settings/**").authenticated()
                .antMatchers("/email-templates/**").authenticated()
                .antMatchers("/email-test/**").authenticated()
                .antMatchers("/notifications/**").authenticated()
                .antMatchers("/notification-templates/**").authenticated()
                .antMatchers("/library-documents/**").authenticated()
                .antMatchers("/certifications/**").authenticated()
                .antMatchers("/maintenance/**").authenticated()
                .antMatchers("/welding-procedure-specifications/**").authenticated()

                // The rest of the endpoints are available for all authorized users
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }


} 