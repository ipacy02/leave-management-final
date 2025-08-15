package leave_management_project.leave_management.Repository;


import leave_management_project.leave_management.enumClass.LeaveStatus;
import leave_management_project.leave_management.model.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveRepository extends JpaRepository<Leave, UUID> {

    List<Leave> findByStartDateBetween(LocalDate from, LocalDate to);

    List<Leave> findByUserId(UUID userId);

    @Query("SELECT l FROM Leave l WHERE l.status = :status AND " +
            "(l.startDate <= :endDate AND l.endDate >= :startDate)")
    List<Leave> findByStatusAndDateRange(@Param("status") LeaveStatus status,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

}

