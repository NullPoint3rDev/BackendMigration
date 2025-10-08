package org.alloy.security;

import org.alloy.models.User;
import org.alloy.models.entities.UserRolePermission;
import org.alloy.repositories.UserRepository;
import org.alloy.repositories.UserRolePermissionRepository;
import org.alloy.repositories.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRolePermissionRepository userRolePermissionRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserRolePermissionRepository userRolePermissionRepository,
                                    UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRolePermissionRepository = userRolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("CustomUserDetailsService: Попытка загрузки пользователя: " + username);
        
        return userRepository.findByUsername(username)
                .map(user -> {
                    System.out.println("CustomUserDetailsService: Пользователь найден в базе: " + username + ", статус: " + user.getStatus());
                    
                    // Проверяем статус пользователя (0 = Active, 1 = Blocked)
                    if (user.getStatus() != null && user.getStatus() == 1) {
                        System.out.println("CustomUserDetailsService: Попытка входа для заблокированного пользователя: " + username);
                        throw new UsernameNotFoundException("User account is blocked");
                    }
                    
                    System.out.println("CustomUserDetailsService: Создаем UserDetails для пользователя: " + username);
                    return createUserDetails(user);
                })
                .orElseThrow(() -> {
                    System.out.println("CustomUserDetailsService: Пользователь не найден в базе: " + username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
    }

    private UserDetails createUserDetails(User user) {
        String password = user.getPassword();
        System.out.println("CustomUserDetailsService: Пароль для пользователя " + user.getUsername() + " получен");
        
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Adding user's role
        if(user.getUserRoleId() != null) {
            // Get role name
            userRoleRepository.findById(user.getUserRoleId()).ifPresent(role -> {
                String roleName = role.getName();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            });

            // Get all permissions for this role
            List<UserRolePermission> rolePermissions = userRolePermissionRepository.findByUserRoleId(user.getUserRoleId());

            // Add every permission
            for(UserRolePermission rolePermission : rolePermissions) {
                String permissionName = rolePermission.getUserPermission().getName();

                if(rolePermission.getRead() != null && rolePermission.getRead()) {
                    authorities.add(new SimpleGrantedAuthority(permissionName + "_READ"));
                }
                if(rolePermission.getWrite() != null && rolePermission.getWrite()) {
                    authorities.add(new SimpleGrantedAuthority(permissionName + "_WRITE"));
                }
            }
        } else {
            // If user doesn't have any role - give him a default role
            authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
        }
        
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            password,
            authorities
        );
    }
} 