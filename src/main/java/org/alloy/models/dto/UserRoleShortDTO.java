package org.alloy.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Краткое представление роли пользователя")
public class UserRoleShortDTO {
    
    @Schema(description = "Уникальный идентификатор роли")
    private Integer id;
    
    @Schema(description = "Название роли")
    private String name;
    
    public UserRoleShortDTO() {}
    
    public UserRoleShortDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
