package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.antenatal.common.ApiException;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Role;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.security.CurrentPrincipal;

@Service
@RequiredArgsConstructor
public class DoctorAccessService {

    private final DoctorRepository doctorRepository;
    private final PatientCardRepository patientCardRepository;

    public Doctor requireDoctor(CurrentPrincipal principal) {
        Long doctorId = principal.requireDoctorId();
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.unauthorized("Врач не найден"));
        if (!"ACTIVE".equals(doctor.getStatus())) {
            throw ApiException.forbidden("Учётная запись отключена");
        }
        return doctor;
    }

    public PatientCard requirePatient(Doctor doctor, Long patientId) {
        if (doctor.getRole() == Role.ADMIN) {
            PatientCard card = patientCardRepository.findById(patientId)
                    .orElseThrow(() -> ApiException.notFound("Пациентка не найдена"));
            if (!card.getClinicId().equals(doctor.getClinicId())) {
                throw ApiException.notFound("Пациентка не найдена");
            }
            return card;
        }
        return patientCardRepository.findByIdAndDoctorId(patientId, doctor.getId())
                .orElseThrow(() -> ApiException.notFound("Пациентка не найдена"));
    }
}
