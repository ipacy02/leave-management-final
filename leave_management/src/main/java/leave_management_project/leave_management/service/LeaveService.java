package leave_management_project.leave_management.service;

import leave_management_project.leave_management.Repository.LeaveRepository;
import leave_management_project.leave_management.Repository.PublicHolidayRepository;
import leave_management_project.leave_management.enumClass.LeaveStatus;
import leave_management_project.leave_management.enumClass.LeaveType;
import leave_management_project.leave_management.model.Leave;
import leave_management_project.leave_management.model.PublicHoliday;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmailService emailService;
    private final PublicHolidayRepository publicHolidayRepository;

    private final Map<UUID, Map<LeaveType, Double>> leaveBalances = new HashMap<>();
    private final Map<UUID, Double> carryoverBalances = new HashMap<>();

    /** ------------------ LEAVE BALANCE ------------------ **/
    private void ensureUserBalance(UUID userId) {
        if (!leaveBalances.containsKey(userId)) {
            initUserBalance(userId);
        }
    }

    public void initUserBalance(UUID userId) {
        Map<LeaveType, Double> balances = new EnumMap<>(LeaveType.class);
        balances.put(LeaveType.ANNUAL, 10.0);
        balances.put(LeaveType.PTO, 20.0);
        balances.put(LeaveType.SICK, 10.0);
        balances.put(LeaveType.COMPASSIONATE, 5.0);
        balances.put(LeaveType.MATERNITY, 90.0);
        balances.put(LeaveType.PATERNITY, 10.0);
        balances.put(LeaveType.UNPAID, 0.0);
        balances.put(LeaveType.OTHER, 5.0);
        leaveBalances.put(userId, balances);
    }

    public void adjustBalance(UUID userId, LeaveType type, double newBalance, String role) {
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("Access denied: Only Admin can adjust leave balances");
        }
        ensureUserBalance(userId);
        leaveBalances.get(userId).put(type, newBalance);
    }

    /** ------------------ LEAVE REQUEST ------------------ **/
    public Leave applyLeave(UUID userId, String userEmail, LeaveType type, LocalDate start, LocalDate end,
                            String reason, MultipartFile document) throws IOException {
        ensureUserBalance(userId);
        long daysRequested = ChronoUnit.DAYS.between(start, end) + 1;
        deductBalance(userId, type, daysRequested);

        String documentPath = saveDocument(document);

        Leave leave = Leave.builder()
                .userId(userId)
                .type(type)
                .status(LeaveStatus.PENDING)
                .startDate(start)
                .endDate(end)
                .reason(reason)
                .documentPath(documentPath)
                .build();

        Leave savedLeave = leaveRepository.save(leave);

        if (userEmail != null && !userEmail.isEmpty()) {
            emailService.sendEmail(
                    userEmail,
                    "Leave Applied",
                    "Your leave request from " + start + " to " + end + " has been submitted and is pending approval."
            );
        }

        return savedLeave;
    }

    /** ------------------ APPROVAL / REJECTION ------------------ **/
    public Leave approveLeave(UUID leaveId, String comment, String role, String managerEmail, String userEmail) {
        if (!"MANAGER".equals(role) && !"ADMIN".equals(role)) {
            throw new RuntimeException("Access denied: Only Manager or Admin can approve leaves");
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setManagerComment(comment);
        Leave savedLeave = leaveRepository.save(leave);

        if (userEmail != null && !userEmail.isEmpty()) {
            emailService.sendEmail(
                    userEmail,
                    "Leave Approved",
                    "Your leave from " + leave.getStartDate() + " to " + leave.getEndDate() +
                            " has been approved by " + managerEmail
            );
        }

        return savedLeave;
    }

    public Leave rejectLeave(UUID leaveId, String comment, String role, String managerEmail, String userEmail) {
        if (!"MANAGER".equals(role) && !"ADMIN".equals(role)) {
            throw new RuntimeException("Access denied: Only Manager or Admin can reject leaves");
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setManagerComment(comment);

        // Refund leave balance
        ensureUserBalance(leave.getUserId());
        long daysRequested = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        Map<LeaveType, Double> balances = leaveBalances.get(leave.getUserId());
        balances.put(leave.getType(), balances.getOrDefault(leave.getType(), 0.0) + daysRequested);

        Leave savedLeave = leaveRepository.save(leave);

        if (userEmail != null && !userEmail.isEmpty()) {
            emailService.sendEmail(
                    userEmail,
                    "Leave Rejected",
                    "Your leave from " + leave.getStartDate() + " to " + leave.getEndDate() +
                            " has been rejected by " + managerEmail
            );
        }

        return savedLeave;
    }

    /** ------------------ FETCH EMAIL ------------------ **/
    public String getUserEmailByLeaveId(UUID leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));
        return getUserEmailById(leave.getUserId());
    }

    private String getUserEmailById(UUID userId) {
        return "user@example.com"; // Replace with real DB/service call
    }

    private String getManagerEmailForUser(UUID userId) {
        return "manager@example.com"; // Replace with real DB/service call
    }

    /** ------------------ CARRYOVER ------------------ **/
    public void processCarryover(UUID userId) {
        ensureUserBalance(userId);
        Map<LeaveType, Double> balances = leaveBalances.get(userId);
        double unusedAnnual = balances.getOrDefault(LeaveType.ANNUAL, 0.0);
        carryoverBalances.put(userId, unusedAnnual * 0.1); // example 10% carryover
        balances.put(LeaveType.ANNUAL, 0.0);
    }

    /** ------------------ HELPER METHODS ------------------ **/
    public Map<LeaveType, Double> viewAllBalances(UUID userId) {
        ensureUserBalance(userId);
        return new HashMap<>(leaveBalances.get(userId));
    }

    public List<Leave> getMyLeaves(UUID userId) {
        return leaveRepository.findByUserId(userId);
    }

    public List<Leave> getAllLeaves() {
        return leaveRepository.findAll();
    }

    private void deductBalance(UUID userId, LeaveType type, double days) {
        ensureUserBalance(userId);
        Map<LeaveType, Double> balances = leaveBalances.get(userId);
        double currentBalance = balances.getOrDefault(type, 0.0);
        if (currentBalance < days) {
            throw new RuntimeException("Insufficient leave balance for type: " + type);
        }
        balances.put(type, currentBalance - days);
    }

    private String saveDocument(MultipartFile document) throws IOException {
        if (document == null || document.isEmpty()) return null;

        String uploadDir = System.getProperty("user.dir") + "/uploads";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String safeFileName = UUID.randomUUID() + "_" +
                document.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        File destinationFile = new File(dir, safeFileName);
        document.transferTo(destinationFile);

        return destinationFile.getAbsolutePath();
    }

    /** ------------------ NEW FEATURE: PUBLIC HOLIDAYS ------------------ **/
    public PublicHoliday addPublicHoliday(PublicHoliday holiday, String role) {
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("Access denied: Only Admin can add public holidays");
        }
        return publicHolidayRepository.save(holiday);
    }

    public List<PublicHoliday> getUpcomingPublicHolidays() {
        return publicHolidayRepository.findByDateAfterOrderByDateAsc(LocalDate.now());
    }

    /** ------------------ NEW FEATURE: COLLEAGUES ON LEAVE ------------------ **/
    public List<Leave> getColleaguesOnLeave(UUID currentUserId, LocalDate start, LocalDate end) {
        return leaveRepository.findByStatusAndDateRange(LeaveStatus.APPROVED, start, end)
                .stream()
                .filter(l -> !l.getUserId().equals(currentUserId))
                .collect(Collectors.toList());
    }
}
