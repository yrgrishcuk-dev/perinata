package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.antenatal.calc.GainCalculator;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.DiaryEntry;
import ru.antenatal.domain.GainCorridor;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.repo.DiaryEntryRepository;
import ru.antenatal.repo.GainCorridorRepository;
import ru.antenatal.web.dto.CommonDtos.CorridorPoint;
import ru.antenatal.web.dto.CommonDtos.GainPoint;
import ru.antenatal.web.dto.CommonDtos.GainView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GainService {

    private final GainCorridorRepository gainCorridorRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final AppProperties appProperties;

    public GainView gain(PatientCard card) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        GainCalculator.GestationalAge age = GainCalculator.gestationalAge(card.getEdd(), today);
        int week = Math.max(1, Math.min(42, age.weeks()));

        GainCorridor corridor = null;
        if (card.getBmiCategory() != null) {
            corridor = gainCorridorRepository
                    .findFirstByBmiCategoryAndWeekOrderByIdDesc(card.getBmiCategory(), week)
                    .orElse(null);
        }

        BigDecimal latestWeight = diaryEntryRepository
                .findFirstByPatientCardIdAndWeightKgIsNotNullOrderByEntryDateDesc(card.getId())
                .map(DiaryEntry::getWeightKg)
                .orElse(null);

        GainCalculator.Assessment assessment = GainCalculator.assess(
                card.getEdd(), today, card.getPrePregnancyWeightKg(), latestWeight,
                corridor == null ? null : corridor.getMinGainKg(),
                corridor == null ? null : corridor.getMaxGainKg());

        List<GainPoint> points = new ArrayList<>();
        if (card.getPrePregnancyWeightKg() != null) {
            List<DiaryEntry> entries = new ArrayList<>(diaryEntryRepository
                    .findTop400ByPatientCardIdOrderByEntryDateDesc(card.getId()));
            Collections.reverse(entries);
            for (DiaryEntry entry : entries) {
                if (entry.getWeightKg() == null) {
                    continue;
                }
                GainCalculator.GestationalAge pointAge =
                        GainCalculator.gestationalAge(card.getEdd(), entry.getEntryDate());
                points.add(new GainPoint(
                        entry.getEntryDate(),
                        entry.getWeightKg(),
                        pointAge.weeks(),
                        entry.getWeightKg().subtract(card.getPrePregnancyWeightKg())));
            }
        }

        List<CorridorPoint> corridorPoints = card.getBmiCategory() == null
                ? List.of()
                : gainCorridorRepository.findByBmiCategoryOrderByWeekAsc(card.getBmiCategory())
                .stream()
                .map(item -> new CorridorPoint(item.getWeek(), item.getMinGainKg(), item.getMaxGainKg()))
                .toList();

        String status = card.getBmiCategory() == null || corridor == null
                ? "UNKNOWN"
                : assessment.status().name();

        return new GainView(
                assessment.age().weeks(),
                assessment.age().days(),
                assessment.age().label(),
                assessment.minGainKg(),
                assessment.maxGainKg(),
                assessment.gainKg(),
                status,
                points,
                corridorPoints);
    }
}
