package org.alloy.security;

import org.alloy.models.entities.UserAccount;
import org.alloy.services.UserAccountService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountService userAccountService;

    public CustomUserDetailsService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountService.getUserAccountByUserName(username)
                .map(this::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    private UserDetails createUserDetails(UserAccount userAccount) {
        // Convert the password hash from byte[] to String for Spring Security
        String password = userAccount.getPasswordHash() != null ? new String(userAccount.getPasswordHash()) : "";
        
        // Create a list of authorities based on user role
        // For now, we'll use a simple ROLE_USER authority
        // In a real application, you would map user roles to Spring Security authorities
        return new User(
                userAccount.getUserName(),
                password,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
} 