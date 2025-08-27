package org.alloy.services;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAccount> getAllUserAccounts() {
        return userAccountRepository.findAll();
    }

    public Optional<UserAccount> getUserAccountById(Integer id) {
        return userAccountRepository.findById(id);
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

    @Transactional
    public UserAccount createUserAccount(UserAccount userAccount) {
        // Set default status if not provided
        if (userAccount.getStatus() == null) {
            userAccount.setStatus(GeneralStatus.Active);
        }

        // Check if username is already used
        Optional<UserAccount> existingUser = userAccountRepository.findByUserName(userAccount.getUserName());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Username '" + userAccount.getUserName() + "' is already in use");
        }

        // Check if email is already used
        if (userAccount.getEmail() != null && !userAccount.getEmail().isEmpty()) {
            Optional<UserAccount> existingEmail = userAccountRepository.findByEmail(userAccount.getEmail());
            if (existingEmail.isPresent()) {
                throw new IllegalArgumentException("Email '" + userAccount.getEmail() + "' is already in use");
            }
        }

        // Encode password if provided
        if (userAccount.getPasswordHash() != null) {
            String encodedPassword = passwordEncoder.encode(new String(userAccount.getPasswordHash()));
            userAccount.setPasswordHash(encodedPassword.getBytes());
        }

        // Set creation date
        userAccount.setDateCreated(LocalDateTime.now());

        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public UserAccount updateUserAccount(UserAccount userAccount) {
        // Check if user account exists
        if (!userAccountRepository.existsById(userAccount.getId())) {
            throw new IllegalArgumentException("User account with ID " + userAccount.getId() + " does not exist");
        }

        // Check if username is already used by another user
        Optional<UserAccount> existingUser = userAccountRepository.findByUserName(userAccount.getUserName());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userAccount.getId())) {
            throw new IllegalArgumentException("Username '" + userAccount.getUserName() + "' is already in use");
        }

        // Check if email is already used by another user
        if (userAccount.getEmail() != null && !userAccount.getEmail().isEmpty()) {
            Optional<UserAccount> existingEmail = userAccountRepository.findByEmail(userAccount.getEmail());
            if (existingEmail.isPresent() && !existingEmail.get().getId().equals(userAccount.getId())) {
                throw new IllegalArgumentException("Email '" + userAccount.getEmail() + "' is already in use");
            }
        }

        // Encode password if provided
        if (userAccount.getPasswordHash() != null) {
            String encodedPassword = passwordEncoder.encode(new String(userAccount.getPasswordHash()));
            userAccount.setPasswordHash(encodedPassword.getBytes());
        }

        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public void deleteUserAccount(Integer id) {
        // Soft delete by setting status to Deleted
        Optional<UserAccount> userAccountOpt = userAccountRepository.findById(id);
        if (userAccountOpt.isPresent()) {
            UserAccount userAccount = userAccountOpt.get();
            userAccount.setStatus(GeneralStatus.Deleted);
            userAccountRepository.save(userAccount);
        } else {
            throw new IllegalArgumentException("User account with ID " + id + " does not exist");
        }
    }

    @Transactional
    public void hardDeleteUserAccount(Integer id) {
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
}
