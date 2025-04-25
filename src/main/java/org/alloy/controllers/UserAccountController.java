package org.alloy.controllers;

import org.alloy.models.entities.UserAccount;
import org.alloy.services.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-accounts")
public class UserAccountController {

    private final UserAccountService userAccountService;

    @Autowired
    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ResponseEntity<List<UserAccount>> getAllUserAccounts() {
        List<UserAccount> userAccounts = userAccountService.getAllUserAccounts();
        return ResponseEntity.ok(userAccounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getUserAccountById(@PathVariable Integer id) {
        return userAccountService.getUserAccountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{userName}")
    public ResponseEntity<UserAccount> getUserAccountByUserName(@PathVariable String userName) {
        return userAccountService.getUserAccountByUserName(userName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserAccount> getUserAccountByEmail(@PathVariable String email) {
        return userAccountService.getUserAccountByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/organization-unit/{organizationUnitId}")
    public ResponseEntity<List<UserAccount>> getUserAccountsByOrganizationUnitId(@PathVariable Integer organizationUnitId) {
        List<UserAccount> userAccounts = userAccountService.getUserAccountsByOrganizationUnitId(organizationUnitId);
        return ResponseEntity.ok(userAccounts);
    }

    @GetMapping("/user-role/{userRoleId}")
    public ResponseEntity<List<UserAccount>> getUserAccountsByUserRoleId(@PathVariable Integer userRoleId) {
        List<UserAccount> userAccounts = userAccountService.getUserAccountsByUserRoleId(userRoleId);
        return ResponseEntity.ok(userAccounts);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserAccount>> searchUserAccounts(
            @RequestParam Integer organizationUnitId,
            @RequestParam String searchTerm) {
        List<UserAccount> userAccounts = userAccountService.searchUserAccounts(organizationUnitId, searchTerm);
        return ResponseEntity.ok(userAccounts);
    }

    @PostMapping
    public ResponseEntity<UserAccount> createUserAccount(@RequestBody UserAccount userAccount) {
        try {
            UserAccount createdUserAccount = userAccountService.createUserAccount(userAccount);
            return new ResponseEntity<>(createdUserAccount, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccount> updateUserAccount(@PathVariable Integer id, @RequestBody UserAccount userAccount) {
        try {
            userAccount.setId(id);
            UserAccount updatedUserAccount = userAccountService.updateUserAccount(userAccount);
            return ResponseEntity.ok(updatedUserAccount);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserAccount(@PathVariable Integer id) {
        try {
            userAccountService.deleteUserAccount(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUserAccount(@PathVariable Integer id) {
        try {
            userAccountService.hardDeleteUserAccount(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<UserAccount> authenticateUser(@RequestBody Map<String, String> credentials) {
        String userName = credentials.get("userName");
        String password = credentials.get("password");

        if (userName == null || password == null) {
            return ResponseEntity.badRequest().build();
        }

        return userAccountService.authenticateUser(userName, password)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
