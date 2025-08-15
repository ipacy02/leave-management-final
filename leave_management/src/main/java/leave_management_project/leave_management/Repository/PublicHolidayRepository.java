package leave_management_project.leave_management.Repository;

import leave_management_project.leave_management.model.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {
    List<PublicHoliday> findByDateAfterOrderByDateAsc(LocalDate date);
}
