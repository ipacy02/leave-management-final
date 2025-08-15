package auth_management_service.auth_management.config;

import auth_management_service.auth_management.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final SecretKey key = Keys.hmacShaKeyFor(
            "my-very-strong-shared-secret-key-for-jwt".getBytes()
    );

    private final int jwtExpirationMs = 864000000; // 10 days

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    // Expose the key for JWT validation in the filter
    public static SecretKey getKey() {
        return key;
    }
}
