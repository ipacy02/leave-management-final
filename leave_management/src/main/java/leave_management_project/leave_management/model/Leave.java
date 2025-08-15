package leave_management_project.leave_management.model;


import jakarta.persistence.*;
import leave_management_project.leave_management.enumClass.LeaveStatus;
import leave_management_project.leave_management.enumClass.LeaveType;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leaves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leave {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId; // From Auth Service

    @Enumerated(EnumType.STRING)
    private LeaveType type; // Annual, Sick, etc.

    @Enumerated(EnumType.STRING)
    private LeaveStatus status; // Pending, Approved, Rejected

    private LocalDate startDate;
    private LocalDate endDate;

    private String reason;
    private String documentPath; // Optional file upload
    private String managerComment;
}

