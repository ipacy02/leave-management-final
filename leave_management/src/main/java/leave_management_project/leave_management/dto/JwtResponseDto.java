package leave_management_project.leave_management.dto;

import lombok.Data;

@Data
public class JwtResponseDto {
    private String token;

    public JwtResponseDto(String token) {
        this.token = token;
    }
}
