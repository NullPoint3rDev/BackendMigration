package org.alloy.security;

import org.alloy.models.GeneralStatus;
import org.alloy.models.User;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRolePermission;
import org.alloy.repositories.UserAccountRepository;
import org.alloy.repositories.UserPermissionGrantRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    /** ponytail: JWT filter hits DB every request; ceiling — до 60s stale roles/block. */
    private static final long USER_DETAILS_CACHE_TTL_MS = 60_000L;

    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRolePermissionRepository userRolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserPermissionGrantRepository userPermissionGrantRepository;
    private final ConcurrentHashMap<String, CachedUserDetails> userDetailsCache = new ConcurrentHashMap<>();

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserAccountRepository userAccountRepository,
                                    UserRolePermissionRepository userRolePermissionRepository,
                                    UserRoleRepository userRoleRepository,
                                    UserPermissionGrantRepository userPermissionGrantRepository) {
        this.userRepository = userRepository;
        this.userAccountRepository = userAccountRepository;
        this.userRolePermissionRepository = userRolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userPermissionGrantRepository = userPermissionGrantRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        long now = System.currentTimeMillis();
        CachedUserDetails cached = userDetailsCache.get(username);
        if (cached != null && now - cached.loadedAtMs < USER_DETAILS_CACHE_TTL_MS) {
            return cached.userDetails;
        }

        System.out.println("CustomUserDetailsService: Попытка загрузки пользователя: " + username);

        Optional<UserAccount> userAccountOpt = userAccountRepository.findByUserName(username);
        if (userAccountOpt.isPresent()) {
            GeneralStatus accountStatus = userAccountOpt.get().getStatus();
            if (accountStatus == GeneralStatus.Blocked || accountStatus == GeneralStatus.Deleted) {
                userDetailsCache.remove(username);
                throw new UsernameNotFoundException("User account is blocked");
            }
        }

        UserDetails details = userRepository.findByUsername(username)
                .map(user -> {
                    System.out.println("CustomUserDetailsService: Пользователь найден в базе: " + username + ", статус: " + user.getStatus());

                    // users.status: 0 = Active, 1 = Blocked, 2 = Deleted
                    if (user.getStatus() != null && (user.getStatus() == 1 || user.getStatus() == 2)) {
                        userDetailsCache.remove(username);
                        throw new UsernameNotFoundException("User account is blocked");
                    }

                    System.out.println("CustomUserDetailsService: Создаем UserDetails для пользователя: " + username);
                    return createUserDetails(user);
                })
                .orElseThrow(() -> {
                    System.out.println("CustomUserDetailsService: Пользователь не найден в базе: " + username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        userDetailsCache.put(username, new CachedUserDetails(details, now));
        return details;
    }

    private static final class CachedUserDetails {
        final UserDetails userDetails;
        final long loadedAtMs;

        CachedUserDetails(UserDetails userDetails, long loadedAtMs) {
            this.userDetails = userDetails;
            this.loadedAtMs = loadedAtMs;
        }
    }

    private UserDetails createUserDetails(User user) {
        String password = user.getPassword();
        System.out.println("CustomUserDetailsService: Пароль для пользователя " + user.getUsername() + " получен");
        System.out.println("CustomUserDetailsService: Длина пароля: " + (password != null ? password.length() : "null"));
        System.out.println("CustomUserDetailsService: Начало пароля: " + (password != null && password.length() > 30 ? password.substring(0, 30) : password));
        System.out.println("CustomUserDetailsService: Пароль как строка: " + password);

        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getUserRoleId() != null) {
            userRoleRepository.findById(user.getUserRoleId()).ifPresent(role -> {
                String roleName = role.getName();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            });

            List<UserRolePermission> rolePermissions = userRolePermissionRepository.findByUserRoleIdWithPermission(user.getUserRoleId());
            Set<Integer> grantedPermissionIds = userPermissionGrantRepository.findByUserId(user.getId()).stream()
                    .map(g -> g.getUserPermissionId())
                    .collect(Collectors.toSet());

            for (UserRolePermission rp : rolePermissions) {
                String permName = rp.getUserPermission().getName();
                String authority = "PERMISSION_" + permName;

                if (rp.getConfigurableByRoleLevel() != null) {
                    if (grantedPermissionIds.contains(rp.getUserPermissionId())) {
                        authorities.add(new SimpleGrantedAuthority(authority));
                    }
                } else if (Boolean.TRUE.equals(rp.getRead()) || Boolean.TRUE.equals(rp.getWrite())) {
                    authorities.add(new SimpleGrantedAuthority(authority));
                }
            }
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
        }

        userAccountRepository.findByUserName(user.getUsername()).ifPresent(account -> {
            Set<String> allowedActions = AllowedUserActionsHelper.parseActions(account);
            for (String permissionAuthority : AllowedUserActionsAuthorityMapper.permissionAuthorities(allowedActions)) {
                authorities.add(new SimpleGrantedAuthority(permissionAuthority));
            }
        });

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                password,
                authorities
        );
    }
} 