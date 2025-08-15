package leave_management_project.leave_management.controller;

import leave_management_project.leave_management.enumClass.LeaveType;
import leave_management_project.leave_management.model.Leave;
import leave_management_project.leave_management.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // Staff: Initialize leave balances for themselves
    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("/init")
    public ResponseEntity<String> initBalance(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        leaveService.initUserBalance(userId);
        return ResponseEntity.ok("Leave balances initialized for user: " + userId);
    }

    // Staff: View their own leave balances
    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/balances")
    public ResponseEntity<Map<LeaveType, Double>> getBalances(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(leaveService.viewAllBalances(userId));
    }

    // Admin: Adjust anyone's balance
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/balances")
    public ResponseEntity<String> adjustBalance(
            @RequestParam UUID userId,
            @RequestParam LeaveType type,
            @RequestParam double newBalance
    ) {
        leaveService.adjustBalance(userId, type, newBalance, "ADMIN");
        return ResponseEntity.ok("Balance updated for user: " + userId);
    }

    // Staff: Apply for leave
    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("/apply")
    public ResponseEntity<Leave> applyLeave(
            @RequestParam LeaveType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reason,
            @RequestParam(required = false) MultipartFile document,
            Authentication authentication
    ) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        String userEmail = getEmailFromAuth(authentication);
        Leave leave = leaveService.applyLeave(userId, userEmail, type, startDate, endDate, reason, document);
        return ResponseEntity.ok(leave);
    }

    // Staff: View their own leave applications
    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/my")
    public ResponseEntity<List<Leave>> getMyLeaves(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(leaveService.getMyLeaves(userId));
    }

    // Manager & Admin: Approve leave (can't approve their own)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/approve/{leaveId}")
    public ResponseEntity<Leave> approveLeave(
            @PathVariable UUID leaveId,
            @RequestParam String comment,
            Authentication authentication
    ) {
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        String managerEmail = getEmailFromAuth(authentication);
        String userEmail = leaveService.getUserEmailByLeaveId(leaveId);

        // Prevent self-approval
        if (managerEmail.equals(userEmail)) {
            return ResponseEntity.badRequest().body(null);
        }

        Leave leave = leaveService.approveLeave(leaveId, comment, role, managerEmail, userEmail);
        return ResponseEntity.ok(leave);
    }

    // Manager & Admin: Reject leave (can't reject their own)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/reject/{leaveId}")
    public ResponseEntity<Leave> rejectLeave(
            @PathVariable UUID leaveId,
            @RequestParam String comment,
            Authentication authentication
    ) {
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        String managerEmail = getEmailFromAuth(authentication);
        String userEmail = leaveService.getUserEmailByLeaveId(leaveId);

        // Prevent self-rejection
        if (managerEmail.equals(userEmail)) {
            return ResponseEntity.badRequest().body(null);
        }

        Leave leave = leaveService.rejectLeave(leaveId, comment, role, managerEmail, userEmail);
        return ResponseEntity.ok(leave);
    }

    // Admin: View all leave requests
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Leave>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    // Staff: Process carryover for themselves
    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("/carryover")
    public ResponseEntity<String> processCarryover(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        leaveService.processCarryover(userId);
        return ResponseEntity.ok("Carryover processed for user: " + userId);
    }

    /** ------------------ HELPER ------------------ **/
    private String getEmailFromAuth(Authentication authentication) {
        return authentication.getName() + "manpaci45@gmail.com"; // replace with real email fetching
    }
}
