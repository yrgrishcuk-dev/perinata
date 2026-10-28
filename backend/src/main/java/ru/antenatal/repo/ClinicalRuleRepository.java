package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.ClinicalRule;

import java.util.Optional;

public interface ClinicalRuleRepository extends JpaRepository<ClinicalRule, Long> {
    Optional<ClinicalRule> findFirstByCodeAndActiveTrueOrderByIdDesc(String code);
}
