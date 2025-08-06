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
import org.alloy.models.dto.UserAccountDTO;
import org.alloy.models.dto.mapper.UserAccountMapper;
import org.alloy.services.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/user-accounts")
@Tag(name = "User Accounts", description = "API для управления учетными записями пользователей. " +
        "Позволяет создавать, просматривать, обновлять и удалять учетные записи пользователей. " +
        "Поддерживает поиск пользователей по различным параметрам: ID, имя пользователя, email, " +
        "организационная единица, роль пользователя. Включает функционал аутентификации пользователей. " +
        "Поддерживает как мягкое, так и жесткое удаление учетных записей.")
@SecurityRequirement(name = "JWT")
public class UserAccountController {

    @PostConstruct
    public void init() {
        System.out.println("UserAccountController initialized!");
    }

    private final UserAccountService userAccountService;

    @Autowired
    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Operation(
            summary = "Получить все учетные записи пользователей",
            description = "Возвращает список всех учетных записей пользователей в системе. " +
                    "Учетные записи возвращаются с полной информацией о пользователе, " +
                    "включая имя пользователя, email, роли, организационную единицу " +
                    "и другие связанные данные."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список учетных записей успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для доступа к списку учетных записей",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<UserAccountDTO>> getAllUserAccounts() {
        List<UserAccountDTO> userAccounts = userAccountService.getAllUserAccounts().stream()
                .map(UserAccountMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userAccounts);
    }

    @Operation(
            summary = "Получить учетную запись по ID",
            description = "Возвращает учетную запись пользователя по его уникальному идентификатору. " +
                    "Если учетная запись не найдена, возвращается 404 ошибка. " +
                    "Возвращаемая информация включает все детали учетной записи."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Учетная запись успешно найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Учетная запись не найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для доступа к учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserAccountDTO> getUserAccountById(
            @Parameter(description = "ID учетной записи", required = true, example = "1")
            @PathVariable Integer id
    ) {
        return userAccountService.getUserAccountById(id)
                .map(UserAccountMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Получить учетную запись по имени пользователя",
            description = "Возвращает учетную запись пользователя по его имени пользователя. " +
                    "Если учетная запись не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Учетная запись успешно найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Учетная запись не найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для доступа к учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/username/{userName}")
    public ResponseEntity<UserAccountDTO> getUserAccountByUserName(
            @Parameter(description = "Имя пользователя", required = true, example = "john.doe")
            @PathVariable String userName
    ) {
        return userAccountService.getUserAccountByUserName(userName)
                .map(UserAccountMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Получить учетную запись по email",
            description = "Возвращает учетную запись пользователя по его email адресу. " +
                    "Если учетная запись не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Учетная запись успешно найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Учетная запись не найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для доступа к учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<UserAccountDTO> getUserAccountByEmail(
            @Parameter(description = "Email адрес", required = true, example = "john.doe@example.com")
            @PathVariable String email
    ) {
        return userAccountService.getUserAccountByEmail(email)
                .map(UserAccountMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Получить учетные записи по ID организационной единицы",
            description = "Возвращает список учетных записей пользователей, " +
                    "принадлежащих к указанной организационной единице."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список учетных записей успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для доступа к списку учетных записей",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/organization-unit/{organizationUnitId}")
    public ResponseEntity<List<UserAccountDTO>> getUserAccountsByOrganizationUnitId(
            @Parameter(description = "ID организационной единицы", required = true, example = "1")
            @PathVariable Integer organizationUnitId
    ) {
        List<UserAccountDTO> userAccounts = userAccountService.getUserAccountsByOrganizationUnitId(organizationUnitId).stream()
                .map(UserAccountMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userAccounts);
    }

    @Operation(
            summary = "Получить учетные записи по ID роли пользователя",
            description = "Возвращает список учетных записей пользователей, " +
                    "имеющих указанную роль пользователя."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список учетных записей успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для доступа к списку учетных записей",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/user-role/{userRoleId}")
    public ResponseEntity<List<UserAccountDTO>> getUserAccountsByUserRoleId(
            @Parameter(description = "ID роли пользователя", required = true, example = "1")
            @PathVariable Integer userRoleId
    ) {
        List<UserAccountDTO> userAccounts = userAccountService.getUserAccountsByUserRoleId(userRoleId).stream()
                .map(UserAccountMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userAccounts);
    }

    @Operation(
            summary = "Поиск учетных записей",
            description = "Выполняет поиск учетных записей пользователей по заданным критериям. " +
                    "Поиск осуществляется в рамках указанной организационной единицы " +
                    "с использованием поискового запроса."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Результаты поиска успешно получены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для поиска учетных записей",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/search")
    public ResponseEntity<List<UserAccountDTO>> searchUserAccounts(
            @Parameter(description = "ID организационной единицы", required = true, example = "1")
            @RequestParam Integer organizationUnitId,

            @Parameter(description = "Поисковый запрос", required = true, example = "john")
            @RequestParam String searchTerm
    ) {
        List<UserAccountDTO> userAccounts = userAccountService.searchUserAccounts(organizationUnitId, searchTerm).stream()
                .map(UserAccountMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userAccounts);
    }

    @Operation(
            summary = "Создать новую учетную запись",
            description = "Создает новую учетную запись пользователя. " +
                    "Учетная запись должна содержать обязательные поля: имя пользователя, " +
                    "email, пароль и другие необходимые данные. " +
                    "После создания учетная запись будет доступна для использования."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Учетная запись успешно создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для создания учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<UserAccountDTO> createUserAccount(
            @Parameter(description = "Данные учетной записи", required = true)
            @RequestBody UserAccountDTO userAccountDTO
    ) {
        UserAccount entity = UserAccountMapper.toEntity(userAccountDTO);
        return new ResponseEntity<>(UserAccountMapper.toDTO(userAccountService.createUserAccount(entity)), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Обновить учетную запись",
            description = "Обновляет существующую учетную запись пользователя по его ID. " +
                    "Можно изменить любые поля учетной записи, кроме ID. " +
                    "Если учетная запись не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Учетная запись успешно обновлена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Учетная запись не найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для обновления учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserAccountDTO> updateUserAccount(
            @Parameter(description = "ID учетной записи", required = true, example = "1")
            @PathVariable Integer id,
            @Parameter(description = "Обновленные данные учетной записи", required = true)
            @RequestBody UserAccountDTO userAccountDTO
    ) {
        UserAccount entity = UserAccountMapper.toEntity(userAccountDTO);
        entity.setId(id);
        return ResponseEntity.ok(UserAccountMapper.toDTO(userAccountService.updateUserAccount(entity)));
    }

    @Operation(
            summary = "Удалить учетную запись (мягкое удаление)",
            description = "Выполняет мягкое удаление учетной записи пользователя по его ID. " +
                    "Если учетная запись не найдена, возвращается 404 ошибка. " +
                    "При мягком удалении запись остается в базе данных, но помечается как удаленная."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Учетная запись успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Учетная запись не найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для удаления учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserAccount(
            @Parameter(description = "ID учетной записи", required = true, example = "1")
            @PathVariable Integer id
    ) {
        try {
            userAccountService.deleteUserAccount(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Удалить учетную запись (жесткое удаление)",
            description = "Выполняет жесткое удаление учетной записи пользователя по его ID. " +
                    "Если учетная запись не найдена, возвращается 404 ошибка. " +
                    "При жестком удалении запись полностью удаляется из базы данных."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Учетная запись успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Учетная запись не найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для удаления учетной записи",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUserAccount(
            @Parameter(description = "ID учетной записи", required = true, example = "1")
            @PathVariable Integer id
    ) {
        try {
            userAccountService.hardDeleteUserAccount(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Аутентификация пользователя",
            description = "Выполняет аутентификацию пользователя по имени пользователя и паролю. " +
                    "При успешной аутентификации возвращает данные учетной записи пользователя."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Аутентификация успешна",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAccountDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные для аутентификации",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверные учетные данные",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/authenticate")
    public ResponseEntity<UserAccountDTO> authenticateUser(
            @Parameter(description = "Учетные данные пользователя", required = true)
            @RequestBody Map<String, String> credentials
    ) {
        Optional<UserAccount> userOpt = userAccountService.authenticateUser(credentials.get("userName"), credentials.get("password"));
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserAccount user = userOpt.get();
        return ResponseEntity.ok(UserAccountMapper.toDTO(user));
    }

    @Operation(
            summary = "Получить текущего пользователя",
            description = "Возвращает данные текущего авторизованного пользователя"
    )
    @GetMapping("/current")
    public ResponseEntity<UserAccountDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Проверяем, что пользователь аутентифицирован и это не anonymous
        if (authentication == null || 
            !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.ok(null); // Возвращаем null вместо 404
        }
        
        String userName = authentication.getName();
        Optional<UserAccount> userOpt = userAccountService.getUserAccountByUserName(userName);
        if (!userOpt.isPresent()) {
            return ResponseEntity.ok(null); // Возвращаем null вместо 404
        }
        UserAccount user = userOpt.get();
        return ResponseEntity.ok(UserAccountMapper.toDTO(user));
    }

    @Operation(
            summary = "Обновить профиль пользователя",
            description = "Обновляет профиль текущего пользователя"
    )
    @PutMapping("/profile")
    public ResponseEntity<UserAccountDTO> updateProfile(@RequestBody UserProfileUpdateDTO profileData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = auth.getName();
        UserAccount userAccount = userAccountService.getUserAccountByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (profileData.getName() != null) userAccount.setName(profileData.getName());
        if (profileData.getPosition() != null) userAccount.setPosition(profileData.getPosition());
        if (profileData.getOrganizationId() != null) userAccount.setOrganizationUnitId(profileData.getOrganizationId());
        if (profileData.getAbout() != null) userAccount.setDescription(profileData.getAbout());
        // socials - если появится поле в UserAccount, добавить сохранение
        // userAccount.setSocials(profileData.getSocials());
        UserAccount updated = userAccountService.updateUserAccount(userAccount);
        return ResponseEntity.ok(UserAccountMapper.toDTO(updated));
    }

    public static class UserProfileUpdateDTO {
        private String name;
        private String position;
        private Integer organizationId;
        private String about;
        private List<SocialLink> socials;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public Integer getOrganizationId() { return organizationId; }
        public void setOrganizationId(Integer organizationId) { this.organizationId = organizationId; }
        public String getAbout() { return about; }
        public void setAbout(String about) { this.about = about; }
        public List<SocialLink> getSocials() { return socials; }
        public void setSocials(List<SocialLink> socials) { this.socials = socials; }
    }

    public static class SocialLink {
        private String type;
        private String url;
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    @Operation(
            summary = "Загрузить фото пользователя",
            description = "Загружает фото для текущего пользователя"
    )
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UUID> uploadPhoto(
            @RequestPart("file") MultipartFile file
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        UserAccount userAccount = userAccountService.getUserAccountByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            UUID photoId = userAccountService.uploadUserPhoto(userAccount.getId(), file);
            return ResponseEntity.ok(photoId);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/photo/{photoId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID photoId) {
        try {
            byte[] photoData = userAccountService.getPhoto(photoId);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(photoData);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "User account with id 1 not found")
        private String message;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
