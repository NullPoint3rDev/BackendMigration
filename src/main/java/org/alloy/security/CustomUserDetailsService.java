package org.alloy.security;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRolePermission;
import org.alloy.repositories.UserRolePermissionRepository;
import org.alloy.services.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountService userAccountService;

    private final UserRolePermissionRepository userRolePermissionRepository;

    public CustomUserDetailsService(UserAccountService userAccountService,
                                    UserRolePermissionRepository userRolePermissionRepository) {
        this.userAccountService = userAccountService;
        this.userRolePermissionRepository = userRolePermissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("CustomUserDetailsService: Попытка загрузки пользователя: " + username);
        
        return userAccountService.getUserAccountByUserName(username)
                .map(userAccount -> {
                    System.out.println("CustomUserDetailsService: Пользователь найден в базе: " + username + ", статус: " + userAccount.getStatus());
                    
                    // Проверяем статус пользователя
                    if (userAccount.getStatus() == GeneralStatus.Deleted || 
                        userAccount.getStatus() == GeneralStatus.Blocked) {
                        System.out.println("CustomUserDetailsService: Попытка входа для заблокированного/удаленного пользователя: " + username + " со статусом: " + userAccount.getStatus());
                        throw new UsernameNotFoundException("User account is " + userAccount.getStatus());
                    }
                    
                    System.out.println("CustomUserDetailsService: Создаем UserDetails для пользователя: " + username);
                    return createUserDetails(userAccount);
                })
                .orElseThrow(() -> {
                    System.out.println("CustomUserDetailsService: Пользователь не найден в базе: " + username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
    }

    private UserDetails createUserDetails(UserAccount userAccount) {
        // Convert the password hash from byte[] to String for Spring Security
        String password = "";
        if (userAccount.getPasswordHash() != null) {
            try {
                // Используем UTF-8 кодировку для корректной конвертации BCrypt хеша
                password = new String(userAccount.getPasswordHash(), "UTF-8");
                System.out.println("CustomUserDetailsService: Пароль для пользователя " + userAccount.getUserName() + " успешно конвертирован");
                System.out.println("CustomUserDetailsService: Длина пароля: " + password.length());
                System.out.println("CustomUserDetailsService: Начало пароля: " + password.substring(0, Math.min(10, password.length())));
                
                // Проверяем, что пароль в правильном формате BCrypt
                if (!password.startsWith("$2a$")) {
                    System.err.println("CustomUserDetailsService: ВНИМАНИЕ! Пароль не в формате BCrypt: " + password.substring(0, Math.min(20, password.length())));
                }
            } catch (Exception e) {
                System.err.println("CustomUserDetailsService: Ошибка конвертации пароля для пользователя " + userAccount.getUserName() + ": " + e.getMessage());
                password = "";
            }
        } else {
            System.err.println("CustomUserDetailsService: Пароль для пользователя " + userAccount.getUserName() + " равен NULL");
        }
        
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Adding user's role
        if(userAccount.getUserRole() != null) {
            String roleName = userAccount.getUserRole().getName();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

            // Get all permissions for this role
            List<UserRolePermission> rolePermissions = userRolePermissionRepository.findByUserRoleId(userAccount.getUserRole().getId());

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
        
        return new User(
            userAccount.getUserName(),
            password,
            authorities
        );
    }
} 