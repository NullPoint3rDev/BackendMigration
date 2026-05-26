package org.alloy.services;

import org.alloy.models.GeneralStatus;
import org.alloy.models.User;
import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.UserAccountRepository;
import org.alloy.security.AllowedUserActionsHelper;
import org.alloy.repositories.UserRepository;
import org.alloy.security.SessionManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Wt2AccessService wt2AccessService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private SessionManagementService sessionManagementService;

    @Autowired
    public UserAccountService(UserAccountRepository userAccountRepository, UserRepository userRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userRepository = userRepository;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /** Статус в таблице users для Spring Security: 0 — активен, 1 — заблокирован, 2 — удалён. */
    private static int toAuthUserStatus(GeneralStatus status) {
        if (status == null || status == GeneralStatus.Active || status == GeneralStatus.Pending) {
            return 0;
        }
        if (status == GeneralStatus.Blocked) {
            return 1;
        }
        if (status == GeneralStatus.Deleted) {
            return 2;
        }
        if (status == GeneralStatus.Inactive) {
            return 3;
        }
        return 0;
    }

    private void syncAuthUserRecord(UserAccount userAccount, String lookupUsername, String rawPasswordOrNull) {
        userRepository.findByUsername(lookupUsername).ifPresent(authUser -> {
            authUser.setUsername(userAccount.getUserName());
            authUser.setEmail(userAccount.getEmail() != null ? userAccount.getEmail() : userAccount.getUserName() + "@local");
            authUser.setUserRoleId(userAccount.getUserRoleId());
            authUser.setStatus(toAuthUserStatus(userAccount.getStatus()));
            if (rawPasswordOrNull != null && userAccount.getPasswordHash() != null) {
                authUser.setPassword(new String(userAccount.getPasswordHash()));
            }
            userRepository.save(authUser);
        });
    }

    public List<UserAccount> getAllUserAccounts() {
        return userAccountRepository.findAllActiveWithOrganization(GeneralStatus.Deleted);
    }

    public Optional<UserAccount> getUserAccountById(Integer id) {
        return userAccountRepository.findById(id);
    }

    public boolean existsById(Integer id) {
        return userAccountRepository.existsById(id);
    }

    public Optional<UserAccount> getUserAccountByUserName(String userName) {
        return userAccountRepository.findByUserName(userName);
    }

    public Optional<UserAccount> getUserAccountByEmail(String email) {
        return userAccountRepository.findByEmail(email);
    }

    public List<UserAccount> getUserAccountsByOrganizationUnitId(Integer organizationUnitId) {
        return userAccountRepository.findByOrganizationUnitId(organizationUnitId);
    }

    public List<UserAccount> getUserAccountsByUserRoleId(Integer userRoleId) {
        return userAccountRepository.findByUserRoleId(userRoleId);
    }

    public List<UserAccount> searchUserAccounts(Integer organizationUnitId, String searchTerm) {
        return userAccountRepository.searchUserAccounts(organizationUnitId, searchTerm);
    }

    public boolean isOwner(Integer userAccountId, String username) {
        Optional<UserAccount> userAccount = getUserAccountById(userAccountId);
        return userAccount.isPresent() && userAccount.get().getUserName().equals(username);
    }

    public boolean hasAllowedUserAction(String username, String actionId) {
        if (username == null || username.isEmpty() || actionId == null || actionId.isEmpty()) {
            return false;
        }
        Optional<UserAccount> userAccountOpt = userAccountRepository.findByUserNameAndStatusNot(username, GeneralStatus.Deleted);
        if (!userAccountOpt.isPresent()) {
            return false;
        }
        UserAccount userAccount = userAccountOpt.get();
        String raw = userAccount.getAllowedUserActions();
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        String needle = actionId.trim().toLowerCase();
        String[] parts = raw.split(",");
        for (String p : parts) {
            if (p == null) continue;
            if (p.trim().toLowerCase().equals(needle)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public UserAccount createUserAccount(UserAccount userAccount) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            wt2AccessService.assertCanCreateOrUpdateUserAccount(userAccount, auth.getName(), null);
        }
        // Set default status if not provided
        if (userAccount.getStatus() == null) {
            userAccount.setStatus(GeneralStatus.Active);
        }

        // Check if username is already used by a non-deleted account
        Optional<UserAccount> existingUser = userAccountRepository.findByUserNameAndStatusNot(
                userAccount.getUserName(), GeneralStatus.Deleted);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Логин '" + userAccount.getUserName() + "' уже используется");
        }

        // Check if email is already used by a non-deleted account
        if (userAccount.getEmail() != null && !userAccount.getEmail().isEmpty()) {
            Optional<UserAccount> existingEmail = userAccountRepository.findByEmailAndStatusNot(
                    userAccount.getEmail(), GeneralStatus.Deleted);
            if (existingEmail.isPresent()) {
                throw new IllegalArgumentException("Email '" + userAccount.getEmail() + "' уже используется");
            }
        }

        // Encode password
        String rawPassword = userAccount.getPasswordHash() != null ? new String(userAccount.getPasswordHash()) : null;
        if (rawPassword != null) {
            String encodedPassword = passwordEncoder.encode(rawPassword);
            userAccount.setPasswordHash(encodedPassword.getBytes());
        }

        // Set creation date
        userAccount.setDateCreated(LocalDateTime.now());

        if (userAccount.getEmailVerified() == null) {
            userAccount.setEmailVerified(false);
        }

        sanitizeAllowedUserActions(userAccount);

        UserAccount saved = userAccountRepository.save(userAccount);

        // Also create a record in the "users" table for Spring Security authentication
        if (rawPassword != null) {
            Optional<User> existingAuthUser = userRepository.findByUsername(userAccount.getUserName());
            User authUser = existingAuthUser.orElse(new User());
            authUser.setUsername(userAccount.getUserName());
            authUser.setPassword(new String(userAccount.getPasswordHash()));
            authUser.setEmail(userAccount.getEmail() != null ? userAccount.getEmail() : userAccount.getUserName() + "@local");
            authUser.setUserRoleId(userAccount.getUserRoleId());
            authUser.setStatus(toAuthUserStatus(saved.getStatus()));
            userRepository.save(authUser);
        }

        return saved;
    }

    @Transactional
    public UserAccount updateUserAccount(UserAccount userAccount) {
        Optional<UserAccount> existingOpt = userAccountRepository.findById(userAccount.getId());
        if (!existingOpt.isPresent()) {
            throw new IllegalArgumentException("User account with ID " + userAccount.getId() + " does not exist");
        }
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            wt2AccessService.assertCanCreateOrUpdateUserAccount(userAccount, auth.getName(), userAccount.getId());
        }
        UserAccount existing = existingOpt.get();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            userAccountRepository.findByUserName(auth.getName()).ifPresent(self -> {
                if (self.getId().equals(userAccount.getId())
                        && userAccount.getStatus() == GeneralStatus.Blocked
                        && existing.getStatus() != GeneralStatus.Blocked) {
                    userAccount.setStatus(existing.getStatus());
                }
            });
        }
        String oldUsername = existing.getUserName();

        // Check if username is already used by another non-deleted user
        Optional<UserAccount> existingUser = userAccountRepository.findByUserNameAndStatusNot(
                userAccount.getUserName(), GeneralStatus.Deleted);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userAccount.getId())) {
            throw new IllegalArgumentException("Логин '" + userAccount.getUserName() + "' уже используется");
        }

        // Check if email is already used by another non-deleted user
        if (userAccount.getEmail() != null && !userAccount.getEmail().isEmpty()) {
            Optional<UserAccount> existingEmail = userAccountRepository.findByEmailAndStatusNot(
                    userAccount.getEmail(), GeneralStatus.Deleted);
            if (existingEmail.isPresent() && !existingEmail.get().getId().equals(userAccount.getId())) {
                throw new IllegalArgumentException("Email '" + userAccount.getEmail() + "' уже используется");
            }
        }

        String rawPassword = null;
        if (userAccount.getPasswordHash() != null) {
            rawPassword = new String(userAccount.getPasswordHash());
            String encodedPassword = passwordEncoder.encode(rawPassword);
            userAccount.setPasswordHash(encodedPassword.getBytes());
        } else {
            userAccount.setPasswordHash(existing.getPasswordHash());
        }

        if (userAccount.getDateCreated() == null) {
            userAccount.setDateCreated(existing.getDateCreated());
        }
        if (userAccount.getPhoto() == null && existing.getPhoto() != null) {
            userAccount.setPhoto(existing.getPhoto());
        }

        String oldEmail = normalizeEmail(existing.getEmail());
        String newEmail = normalizeEmail(userAccount.getEmail());
        if (!java.util.Objects.equals(oldEmail, newEmail)) {
            emailVerificationService.deleteByUserAccountId(existing.getId());
            userAccount.setEmailVerified(false);
        } else {
            userAccount.setEmailVerified(Boolean.TRUE.equals(existing.getEmailVerified()));
        }

        sanitizeAllowedUserActions(userAccount);

        UserAccount saved = userAccountRepository.save(userAccount);

        syncAuthUserRecord(saved, oldUsername, rawPassword);

        if (saved.getStatus() == GeneralStatus.Blocked || saved.getStatus() == GeneralStatus.Deleted) {
            sessionManagementService.removeUserSessions(saved.getUserName());
        }

        return saved;
    }

    @Transactional
    public void deleteUserAccount(Integer id) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            wt2AccessService.assertCanViewUserAccount(id, auth.getName());
        }
        Optional<UserAccount> userAccountOpt = userAccountRepository.findById(id);
        if (userAccountOpt.isPresent()) {
            UserAccount userAccount = userAccountOpt.get();
            String originalUsername = userAccount.getUserName();
            userAccount.setStatus(GeneralStatus.Deleted);
            String suffix = "_deleted_" + id;
            userAccount.setUserName(originalUsername + suffix);
            if (userAccount.getEmail() != null && !userAccount.getEmail().isEmpty()) {
                userAccount.setEmail(userAccount.getEmail() + suffix);
            }
            userAccountRepository.save(userAccount);

            // Also mark the "users" table record so the user can't log in
            userRepository.findByUsername(originalUsername).ifPresent(authUser -> {
                authUser.setUsername(originalUsername + suffix);
                authUser.setStatus(2); // Deleted
                userRepository.save(authUser);
            });
        } else {
            throw new IllegalArgumentException("User account with ID " + id + " does not exist");
        }
    }

    @Transactional
    public void hardDeleteUserAccount(Integer id) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            wt2AccessService.assertCanViewUserAccount(id, auth.getName());
        }
        userAccountRepository.deleteById(id);
    }

    @Transactional
    public Optional<UserAccount> authenticateUser(String userName, String password) {
        Optional<UserAccount> userAccountOpt = userAccountRepository.findByUserName(userName);
        if (userAccountOpt.isPresent()) {
            UserAccount userAccount = userAccountOpt.get();

            // Проверяем статус пользователя
            if (userAccount.getStatus() == GeneralStatus.Deleted || userAccount.getStatus() == GeneralStatus.Blocked) {
                System.out.println("Попытка входа для заблокированного/удаленного пользователя: " + userName);
                return Optional.empty();
            }

            if (passwordEncoder.matches(password, new String(userAccount.getPasswordHash()))) {
                // Update last login date
                userAccount.setDateLastLogon(LocalDateTime.now());
                userAccount.setFailedLoginsCount(0);
                return Optional.of(userAccountRepository.save(userAccount));
            } else {
                // Increment failed login count
                userAccount.setFailedLoginsCount(userAccount.getFailedLoginsCount() + 1);
                userAccountRepository.save(userAccount);
            }
        }
        return Optional.empty();
    }

    public UUID savePhoto(String username, MultipartFile photo) throws IOException {
        UserAccount user = userAccountRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID photoId = fileStorageService.saveFile(photo);
        user.setPhoto(photoId);
        userAccountRepository.save(user);

        return photoId;
    }

    public UUID uploadUserPhoto(Integer userId, MultipartFile file) throws IOException {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID photoId = fileStorageService.saveFile(file);
        user.setPhoto(photoId);
        userAccountRepository.save(user);

        return photoId;
    }

    public byte[] getPhoto(UUID photoId) throws IOException {
        return fileStorageService.getFile(photoId);
    }

    private static void sanitizeAllowedUserActions(UserAccount userAccount) {
        if (userAccount == null || userAccount.getAllowedUserActions() == null) {
            return;
        }
        userAccount.setAllowedUserActions(
                AllowedUserActionsHelper.sanitizeAllowedUserActionsCsv(userAccount.getAllowedUserActions()));
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String t = email.trim();
        return t.isEmpty() ? null : t;
    }
}
