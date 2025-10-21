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
            "http://alloynn.keenetic.name:*"
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
            // Public endpoint are available for all type of users (login page, swagger docs, websocket for welding machines)
            .antMatchers("/auth/**", "/login/**", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**").permitAll()

            // User accounts — granular rules
            // Owner/self and common user endpoints (method-level @PreAuthorize will enforce ownership)
            .antMatchers(HttpMethod.GET, "/user-accounts/current").authenticated()
            .antMatchers(HttpMethod.PUT, "/user-accounts/profile").authenticated()
            .antMatchers(HttpMethod.POST, "/user-accounts/photo").authenticated()
            .antMatchers(HttpMethod.GET, "/user-accounts/photo/**").authenticated()
            .antMatchers(HttpMethod.GET, "/user-accounts/username/**").authenticated()
            .antMatchers(HttpMethod.GET, "/user-accounts/email/**").authenticated()

            // Read endpoints for admins and managers
            .antMatchers(HttpMethod.GET, "/user-accounts").hasAnyRole("Администратор", "Менеджер")
            .antMatchers(HttpMethod.GET, "/user-accounts/search").hasAnyRole("Администратор", "Менеджер")
            .antMatchers(HttpMethod.GET, "/user-accounts/user-role/**").hasAnyRole("Администратор", "Менеджер")
            .antMatchers(HttpMethod.GET, "/user-accounts/organization-unit/**").hasAnyRole("Администратор", "Менеджер")

            // Create/Update/Delete restricted to admins
            .antMatchers(HttpMethod.POST, "/user-accounts/**").hasRole("Администратор")
            .antMatchers(HttpMethod.PUT, "/user-accounts/**").hasRole("Администратор")
            .antMatchers(HttpMethod.DELETE, "/user-accounts/**").hasRole("Администратор")

            // Fallback for any other user-accounts paths (if any remain)
            .antMatchers("/user-accounts/**").authenticated()
            .antMatchers("/user-roles/**").hasRole("Администратор")
            .antMatchers("/user-permissions/**").hasRole("Администратор")
            .antMatchers("/user-role-permissions/**").hasRole("Администратор")

            // Reports - admins and managers
            .antMatchers(HttpMethod.GET, "/reports/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers(HttpMethod.POST, "/reports/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers(HttpMethod.PUT, "/reports/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers(HttpMethod.DELETE, "/reports/**").hasRole("Администратор")
            .antMatchers(HttpMethod.DELETE,"/automated-reports/**").hasRole("Администратор")
            .antMatchers(HttpMethod.GET, "/automated-reports/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers(HttpMethod.POST, "/automated-reports/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers(HttpMethod.PUT, "/automated-reports/**").hasAnyRole("Администратор", "Менеджер", "Технолог")

            // Machines - admins, managers, technicians
            .antMatchers("/devices/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers("/welding-devices/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers("/welding-machines/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers("/device-test/**").hasRole("Администратор")

            // Employees - admins and managers; Welders granular to match controller
            .antMatchers("/employees/**").hasAnyRole("Администратор", "Менеджер")
            .antMatchers(HttpMethod.GET, "/welders/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers(HttpMethod.POST, "/welders/**").hasAnyRole("Администратор", "Технолог")
            .antMatchers(HttpMethod.PUT, "/welders/**").hasAnyRole("Администратор", "Технолог")
            .antMatchers(HttpMethod.DELETE, "/welders/**").hasRole("Администратор")

            // Organization — granular to include technologist for reads
            .antMatchers(HttpMethod.GET, "/organizations/**").hasAnyRole("Администратор", "Менеджер", "Технолог")
            .antMatchers(HttpMethod.POST, "/organizations/**").hasAnyRole("Администратор", "Менеджер")
            .antMatchers(HttpMethod.PUT, "/organizations/**").hasAnyRole("Администратор", "Менеджер")
            .antMatchers(HttpMethod.DELETE, "/organizations/**").hasAnyRole("Администратор", "Менеджер")
            .antMatchers("/organization-units/**").hasAnyRole("Администратор", "Менеджер")

            // System settings - only admins
            .antMatchers("/system-settings/**").hasRole("Администратор")
            .antMatchers("/email-templates/**").hasRole("Администратор")
            .antMatchers("/email-test/**").hasRole("Администратор")

            // Notifications - all authorized users 
            .antMatchers("/notifications/**").authenticated()
            .antMatchers("/notification-templates/**").hasAnyRole("Администратор", "Менеджер")

            // Library (documents) - all authorized users
            .antMatchers("/library-documents/**").authenticated()

            // The rest of the endpoints are available for all authorized users
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }


} 