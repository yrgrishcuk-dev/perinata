package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.calc.EddCalculator;
import ru.antenatal.calc.GainCalculator;
import ru.antenatal.common.ApiException;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.EddMethod;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.web.dto.DoctorDtos.PatientDetails;
import ru.antenatal.web.dto.EddDtos.EddCalcRequest;
import ru.antenatal.web.dto.EddDtos.EddPreview;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class EddService {

    private final PatientCardRepository patientCardRepository;
    private final PatientCardService patientCardService;
    private final DoctorAccessService doctorAccessService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public EddPreview preview(Doctor doctor, Long patientId, EddCalcRequest request) {
        doctorAccessService.requirePatient(doctor, patientId);
        return calculate(request);
    }

    @Transactional
    public PatientDetails apply(Doctor doctor, Long patientId, EddCalcRequest request) {
        PatientCard card = doctorAccessService.requirePatient(doctor, patientId);
        EddPreview preview = calculate(request);
        card.setEdd(preview.edd());
        card.setEddMethod(EddMethod.LMP);
        card.setEddBasisDate(preview.lmpDate());
        card.setEddCycleLength(preview.cycleLength());
        card.setEddCalculatedAt(Instant.now());
        patientCardRepository.save(card);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "EDD_CALCULATE", "PATIENT_CARD", String.valueOf(card.getId()),
                "method=LMP");
        return patientCardService.details(doctor, patientId);
    }

    private EddPreview calculate(EddCalcRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of(appProperties.getClinicTimezone()));
        if (request.lmpDate().isAfter(today)) {
            throw ApiException.badRequest("Дата менструации не может быть в будущем");
        }
        int cycleLength = request.cycleLength() == null ? 28 : request.cycleLength();
        if (cycleLength < 21 || cycleLength > 35) {
            throw ApiException.badRequest("Длина цикла должна быть от 21 до 35 дней");
        }
        LocalDate edd = EddCalculator.byLmp(request.lmpDate(), cycleLength);
        patientCardService.validateEdd(edd);
        int week = GainCalculator.gestationalAge(edd, today).weeks();
        return new EddPreview(edd, request.lmpDate(), cycleLength, week, EddMethod.LMP);
    }
}
