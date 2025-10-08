package org.alloy.config;

import static org.springframework.security.config.http.MatcherType.ant;

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
            .antMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**").permitAll()

            // Managing user's account - only for admins
            .antMatchers("/user-accounts/**").hasRole("ADMIN")
            .antMatchers("/user-roles/**").hasRole("ADMIN")
            .antMatchers("/user-permissions/**").hasRole("ADMIN")
            .antMatchers("/user-role-permissions/**").hasRole("ADMIN")

            // Reports - admins and managers
            .antMatchers("/reports/**").hasAnyRole("ADMIN", "MANAGER")
            .antMatchers("/automated-reports/**").hasAnyRole("ADMIN", "MANAGER")

            // Machines - admins, managers, technicians
            .antMatchers("/devices/**").hasAnyRole("ADMIN", "MANAGER", "TECHNOLOGIST")
            .antMatchers("/welding-devices/**").hasAnyRole("ADMIN", "MANAGER", "TECHNOLOGIST")
            .antMatchers("/welding-machines/**").hasAnyRole("ADMIN", "MANAGER", "TECHNOLOGIST")

            // Employees - admins and managers
            .antMatchers("/employees/**").hasAnyRole("ADMIN", "MANAGER")
            .antMatchers("/welders/**").hasAnyRole("ADMIN", "MANAGER")

            // Organization - admins and managers
            .antMatchers("/organizations/**").hasAnyRole("ADMIN", "MANAGER")
            .antMatchers("/organization-units/**").hasAnyRole("ADMIN", "MANAGER")

            // System settings - only admins
            .antMatchers("/system-settings/**").hasRole("ADMIN")
            .antMatchers("/email-templates/**").hasRole("ADMIN")
            .antMatchers("/email-test/**").hasRole("ADMIN")

            // Notifications - all authorized users 
            .antMatchers("/notifications/**").authenticated()
            .antMatchers("/notification-templates/**").hasAnyRole("ADMIN", "MANAGER")

            // Library (documents) - all authorized users
            .antMatchers("/library-documents/**").authenticated()

            // The rest of the endpoints are available for all authorized users
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }


} 