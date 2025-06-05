package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.UserAccount;
import org.alloy.security.AccountLockedException;
import org.alloy.security.AuthenticationService;
import org.alloy.security.PasswordValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "API для аутентификации и управления сессиями пользователей")
public class AuthController {

    @PostConstruct
    public void init() {
        System.out.println("AuthController initialized!");
    }
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/ping")
    public String ping() {
        System.out.println("PING CALLED");
        return "pong";
    }

    @Operation(
        summary = "Аутентификация пользователя",
        description = "Выполняет вход пользователя в систему и возвращает JWT токен и ID сессии. " +
                     "При неудачной попытке входа увеличивает счетчик неудачных попыток. " +
                     "После определенного количества неудачных попыток аккаунт блокируется."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешная аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Неверные учетные данные",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Аккаунт заблокирован",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
        @Parameter(description = "Данные для входа", required = true)
        @Valid @RequestBody LoginRequest loginRequest,
        
        @Parameter(description = "HTTP запрос для получения IP адреса и User-Agent")
        HttpServletRequest request
    ) {
        System.out.println("LOGIN ENDPOINT CALLED");
        try {
            AuthenticationService.AuthenticationResponse response = authenticationService.authenticate(
                    loginRequest.getUsername(), loginRequest.getPassword(), request);

            System.out.println("token: " + response.getToken() + ", sessionId: " + response.getSessionId());

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("token", response.getToken());
            responseMap.put("sessionId", response.getSessionId());
            
            return ResponseEntity.ok(responseMap);
        } catch (AccountLockedException e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Account Locked");
            errorMap.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMap);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Authentication Failed");
            errorMap.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMap);
        }
    }

    @Operation(
        summary = "Выход из системы",
        description = "Выполняет выход пользователя из системы, инвалидируя текущий JWT токен. " +
                     "Требуется действительный JWT токен в заголовке Authorization."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешный выход из системы",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LogoutResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Недействительный токен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка сервера",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @SecurityRequirement(name = "JWT")
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(
        @Parameter(description = "JWT токен в формате 'Bearer {token}'", required = true)
        @RequestHeader("Authorization") String token
    ) {
        try {
            String jwt = token.substring(7);
            authenticationService.logout(jwt);
            return ResponseEntity.ok().body(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed", "message", e.getMessage()));
        }
    }

    @Operation(
        summary = "Проверка пароля",
        description = "Проверяет соответствие пароля требованиям безопасности. " +
                     "Проверяет длину, наличие специальных символов, цифр и букв разного регистра."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пароль соответствует требованиям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PasswordValidationResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Пароль не соответствует требованиям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PasswordValidationErrorResponse.class))
        )
    })
    @PostMapping("/validate-password")
    public ResponseEntity<?> validatePassword(
        @Parameter(description = "Пароль для проверки", required = true)
        @RequestBody PasswordValidationRequest request
    ) {
        try {
            authenticationService.validatePassword(request.getPassword());
            return ResponseEntity.ok().body(Map.of("message", "Password is valid"));
        } catch (PasswordValidationException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Password Validation Failed");
            response.put("message", e.getMessage());
            response.put("validationErrors", e.getValidationErrors());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
        summary = "Регистрация нового пользователя",
        description = "Создает нового пользователя в системе. " +
                     "Проверяет уникальность имени пользователя и соответствие пароля требованиям безопасности."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Пользователь успешно зарегистрирован",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = RegisterResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные для регистрации",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
        @Parameter(description = "Данные для регистрации", required = true)
        @Valid @RequestBody RegisterRequest registerRequest
    ) {
        try {
            UserAccount user = authenticationService.register(
                registerRequest.getUsername(),
                registerRequest.getPassword(),
                registerRequest.getEmail()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Registration Failed");
            errorMap.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    @Schema(description = "Запрос на аутентификацию")
    public static class LoginRequest {
        @Schema(description = "Имя пользователя", example = "john.doe", required = true)
        private String username;

        @Schema(description = "Пароль пользователя", example = "StrongP@ssw0rd", required = true)
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @Schema(description = "Запрос на проверку пароля")
    public static class PasswordValidationRequest {
        @Schema(description = "Пароль для проверки", example = "StrongP@ssw0rd", required = true)
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @Schema(description = "Ответ при успешной аутентификации")
    public static class LoginResponse {
        @Schema(description = "JWT токен для аутентификации", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String token;

        @Schema(description = "Уникальный идентификатор сессии", example = "550e8400-e29b-41d4-a716-446655440000")
        private String sessionId;
    }

    @Schema(description = "Ответ при успешном выходе из системы")
    public static class LogoutResponse {
        @Schema(description = "Сообщение об успешном выходе", example = "Logged out successfully")
        private String message;
    }

    @Schema(description = "Ответ при ошибке")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Authentication Failed")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Invalid username or password")
        private String message;
    }

    @Schema(description = "Ответ при ошибке валидации пароля")
    public static class PasswordValidationErrorResponse {
        @Schema(description = "Тип ошибки", example = "Password Validation Failed")
        private String error;

        @Schema(description = "Общее сообщение об ошибке", example = "Password does not meet security requirements")
        private String message;

        @Schema(description = "Список конкретных ошибок валидации", example = "[\"Password must be at least 8 characters long\", \"Password must contain at least one uppercase letter\"]")
        private List<String> validationErrors;
    }

    @Schema(description = "Ответ при успешной валидации пароля")
    public static class PasswordValidationResponse {
        @Schema(description = "Сообщение об успешной валидации", example = "Password is valid")
        private String message;
    }

    @Schema(description = "Запрос на регистрацию")
    public static class RegisterRequest {
        @Schema(description = "Имя пользователя", example = "john.doe", required = true)
        private String username;

        @Schema(description = "Пароль пользователя", example = "StrongP@ssw0rd", required = true)
        private String password;

        @Schema(description = "Email пользователя", example = "john.doe@example.com", required = true)
        private String email;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @Schema(description = "Ответ при успешной регистрации")
    public static class RegisterResponse {
        @Schema(description = "Сообщение об успешной регистрации", example = "User registered successfully")
        private String message;

        @Schema(description = "ID созданного пользователя", example = "1")
        private Long userId;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
} 