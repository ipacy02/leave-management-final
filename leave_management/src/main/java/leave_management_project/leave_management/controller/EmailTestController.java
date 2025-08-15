package leave_management_project.leave_management.controller;

import leave_management_project.leave_management.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @GetMapping("/test-email")
    public String testEmail() {
        try {
            emailService.sendEmail(
                    "recipient@example.com",  // <-- Replace with your own email to test
                    "Test Email from Leave Management",
                    "Hello! This is a test email to verify SMTP configuration."
            );
            return "Test email sent successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send test email: " + e.getMessage();
        }
    }
}
