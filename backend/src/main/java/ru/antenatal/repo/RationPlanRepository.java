package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.BmiCategory;
import ru.antenatal.domain.RationPlan;

import java.util.Optional;

public interface RationPlanRepository extends JpaRepository<RationPlan, Long> {
    Optional<RationPlan> findFirstByBmiCategoryOrderByIdDesc(BmiCategory bmiCategory);
}
