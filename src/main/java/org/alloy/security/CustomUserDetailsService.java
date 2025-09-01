package org.alloy.security;

import org.alloy.models.GeneralStatus;
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