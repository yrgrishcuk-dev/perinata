package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.antenatal.domain.WeekContent;

import java.util.List;

public interface WeekContentRepository extends JpaRepository<WeekContent, Long> {

    @Query("select w from WeekContent w where w.weekFrom <= :week and w.weekTo >= :week order by w.id desc")
    List<WeekContent> findForWeek(@Param("week") int week);
}
