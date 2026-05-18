package org.alloy.services;

import org.alloy.models.entities.EmailVerificationChallenge;
import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.EmailVerificationChallengeRepository;
import org.alloy.repositories.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int TTL_MINUTES = 10;

    private final EmailVerificationChallengeRepository challengeRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailService emailService;
    private final Wt2AccessService wt2AccessService;

    @Value("${app.email.verification.pepper:weldtelecom-email-verification-pepper}")
    private String pepper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public EmailVerificationService(EmailVerificationChallengeRepository challengeRepository,
                                    UserAccountRepository userAccountRepository,
                                    EmailService emailService,
                                    Wt2AccessService wt2AccessService) {
        this.challengeRepository = challengeRepository;
        this.userAccountRepository = userAccountRepository;
        this.emailService = emailService;
        this.wt2AccessService = wt2AccessService;
    }

    @Transactional
    public void sendVerificationCode(Integer userAccountId, String principalName) {
        UserAccount ua = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        wt2AccessService.assertCanCreateOrUpdateUserAccount(ua, principalName, userAccountId);

        String email = ua.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("У пользователя не указан email");
        }
        email = email.trim();
        if (Boolean.TRUE.equals(ua.getEmailVerified())) {
            throw new IllegalArgumentException("Email уже подтверждён");
        }

        String code = generateSixDigitCode();
        String hash = sha256Hex(code + "|" + userAccountId + "|" + pepper);

        challengeRepository.deleteByUserAccountId(userAccountId);
        EmailVerificationChallenge ch = new EmailVerificationChallenge();
        ch.setUserAccountId(userAccountId);
        ch.setCodeHash(hash);
        ch.setExpiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES));
        challengeRepository.save(ch);

        String displayName = ua.getName() != null && !ua.getName().isEmpty() ? ua.getName() : ua.getUserName();
        String message = String.format(
                "Код подтверждения email в системе WeldTelecom: %s\n\nКод действует %d минут.",
                code, TTL_MINUTES);
        emailService.sendSimpleNotification(email, displayName, "Подтверждение email WeldTelecom", message);
    }

    @Transactional
    public UserAccount confirmVerificationCode(Integer userAccountId, String codeRaw, String principalName) {
        if (codeRaw == null || codeRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("Введите код");
        }
        String code = codeRaw.trim().replaceAll("\\s+", "");
        if (!code.matches("\\d{6}")) {
            throw new IllegalArgumentException("Код должен состоять из 6 цифр");
        }

        UserAccount ua = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        wt2AccessService.assertCanCreateOrUpdateUserAccount(ua, principalName, userAccountId);

        if (Boolean.TRUE.equals(ua.getEmailVerified())) {
            throw new IllegalArgumentException("Email уже подтверждён");
        }
        if (ua.getEmail() == null || ua.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("У пользователя не указан email");
        }

        EmailVerificationChallenge ch = challengeRepository.findByUserAccountId(userAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Код не запрашивался или устарел. Запросите новый код."));

        if (ch.getExpiresAt().isBefore(LocalDateTime.now())) {
            challengeRepository.delete(ch);
            throw new IllegalArgumentException("Срок действия кода истёк. Запросите новый код.");
        }

        String expectedHash = sha256Hex(code + "|" + userAccountId + "|" + pepper);
        if (!expectedHash.equalsIgnoreCase(ch.getCodeHash())) {
            throw new IllegalArgumentException("Неверный код");
        }

        challengeRepository.delete(ch);
        ua.setEmailVerified(true);
        return userAccountRepository.save(ua);
    }

    private String generateSixDigitCode() {
        int n = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(n);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Transactional
    public void deleteByUserAccountId(Integer userAccountId) {
        challengeRepository.deleteByUserAccountId(userAccountId);
    }
}
