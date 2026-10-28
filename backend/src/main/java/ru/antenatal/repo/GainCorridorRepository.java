package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.BmiCategory;
import ru.antenatal.domain.GainCorridor;

import java.util.Optional;

public interface GainCorridorRepository extends JpaRepository<GainCorridor, Long> {
    Optional<GainCorridor> findFirstByBmiCategoryAndWeekOrderByIdDesc(BmiCategory bmiCategory, int week);

    java.util.List<GainCorridor> findByBmiCategoryOrderByWeekAsc(BmiCategory bmiCategory);
}
