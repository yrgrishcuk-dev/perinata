package ru.antenatal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.antenatal.calc.GainCalculator;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.RationPlan;
import ru.antenatal.domain.WeekContent;
import ru.antenatal.repo.RationPlanRepository;
import ru.antenatal.repo.WeekContentRepository;
import ru.antenatal.web.dto.CommonDtos.RationView;
import ru.antenatal.web.dto.CommonDtos.WeekContentView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final WeekContentRepository weekContentRepository;
    private final RationPlanRepository rationPlanRepository;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public Optional<WeekContentView> weekContent(int week) {
        return weekContentRepository.findForWeek(week).stream().findFirst().map(this::toView);
    }

    public Optional<RationView> ration(PatientCard card) {
        if (card.getBmiCategory() == null) {
            return Optional.empty();
        }
        return rationPlanRepository.findFirstByBmiCategoryOrderByIdDesc(card.getBmiCategory())
                .map(this::toView);
    }

    public int currentWeek(PatientCard card) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        return GainCalculator.gestationalAge(card.getEdd(), today).weeks();
    }

    private WeekContentView toView(WeekContent content) {
        return new WeekContentView(
                content.getId(),
                content.getWeekFrom(),
                content.getWeekTo(),
                content.getVersion(),
                content.getTitle(),
                content.getFetalText(),
                content.getNutritionText(),
                content.getWarningText());
    }

    private RationView toView(RationPlan plan) {
        JsonNode meals;
        try {
            meals = objectMapper.readTree(plan.getMeals());
        } catch (Exception e) {
            meals = objectMapper.createArrayNode();
        }
        return new RationView(
                plan.getId(),
                plan.getBmiCategory().name(),
                plan.getBmiCategory().getLabel(),
                plan.getVersion(),
                plan.getTitle(),
                plan.getKcal(),
                plan.getProteinG(),
                plan.getFatG(),
                plan.getCarbsG(),
                meals);
    }
}
