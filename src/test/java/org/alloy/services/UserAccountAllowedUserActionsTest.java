package org.alloy.services;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.UserAccountRepository;
import org.alloy.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountAllowedUserActionsTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserRepository userRepository;

    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(userAccountRepository, userRepository);
    }

    @Test
    void hasAllowedUserAction_falseWhenUserMissing() {
        when(userAccountRepository.findByUserNameAndStatusNot(eq("u"), eq(GeneralStatus.Deleted)))
                .thenReturn(Optional.empty());
        assertFalse(service.hasAllowedUserAction("u", "create_delete_enterprises"));
    }

    @Test
    void hasAllowedUserAction_falseWhenRawEmpty() {
        UserAccount ua = new UserAccount();
        ua.setAllowedUserActions(null);
        when(userAccountRepository.findByUserNameAndStatusNot(eq("u"), eq(GeneralStatus.Deleted)))
                .thenReturn(Optional.of(ua));
        assertFalse(service.hasAllowedUserAction("u", "create_delete_enterprises"));
    }

    @ParameterizedTest
    @CsvSource({
            "create_delete_enterprises, create_delete_enterprises, true",
            "CREATE_DELETE_ENTERPRISES, create_delete_enterprises, true",
            "'  create_delete_enterprises  ', create_delete_enterprises, true",
            "other, create_delete_enterprises, false",
            "create_delete_enterprises, other, false"
    })
    void hasAllowedUserAction_commaSeparatedCaseInsensitive(String stored, String requested, boolean expected) {
        UserAccount ua = new UserAccount();
        ua.setAllowedUserActions(stored);
        when(userAccountRepository.findByUserNameAndStatusNot(eq("u"), eq(GeneralStatus.Deleted)))
                .thenReturn(Optional.of(ua));
        assertEquals(expected, service.hasAllowedUserAction("u", requested));
    }

    @Test
    void hasAllowedUserAction_multipleEntries() {
        UserAccount ua = new UserAccount();
        ua.setAllowedUserActions("foo, create_delete_enterprises, bar");
        when(userAccountRepository.findByUserNameAndStatusNot(any(), eq(GeneralStatus.Deleted)))
                .thenReturn(Optional.of(ua));
        assertTrue(service.hasAllowedUserAction("u", "create_delete_enterprises"));
    }
}
