package leave_management_project.leave_management.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class JwtTest {
    public static void main(String[] args) {
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MTUyMjE2MC05MDY1LTQwNmEtOGJiNy1jZWVmNGQwNDAzYzQiLCJyb2xlIjoiU1RBRkYiLCJpYXQiOjE3NTUxODc5MjksImV4cCI6MTc1NjA1MTkyOX0.PcaLGzedo-Ztcf73SpNkG12nKHn5HX-7Idf1ng08p1Q";

        // 🔹 This must be EXACTLY the same key as in Auth Service
        String secret = "my-very-strong-shared-secret-key-for-jwt";

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            System.out.println("✅ Token is valid");
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Role: " + claims.get("role"));
            System.out.println("Issued At: " + claims.getIssuedAt());
            System.out.println("Expiration: " + claims.getExpiration());

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.out.println("❌ Token is expired: " + e.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException e) {
            System.out.println("❌ Invalid token signature: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Token invalid: " + e.getMessage());
        }
    }
}
