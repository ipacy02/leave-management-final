package auth_management_service.auth_management.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String email;
    private String password;
}
