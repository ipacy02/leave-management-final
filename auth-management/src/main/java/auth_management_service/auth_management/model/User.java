package auth_management_service.auth_management.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    private String username;
    private String email;
    private String password;
    private String role; // STAFF, MANAGER, ADMIN
    private boolean twoFactorEnabled;
    private String googleAvatarUrl;
}
