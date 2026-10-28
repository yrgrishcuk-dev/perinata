package ru.antenatal.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.calc.BmiCalculator;
import ru.antenatal.domain.Clinic;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.PatientAccount;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.Platform;
import ru.antenatal.domain.Role;
import ru.antenatal.repo.ClinicRepository;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.repo.PatientAccountRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.service.LinkService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final PatientCardRepository patientCardRepository;
    private final PatientAccountRepository patientAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(String... args) {
        Clinic clinic = clinicRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            Clinic created = new Clinic();
            created.setName("КДЦ ПЦ");
            created.setTimezone(appProperties.getClinicTimezone());
            return clinicRepository.save(created);
        });

        seedAdmin(clinic);
        if (appProperties.isDevMode()) {
            log.warn("DEV_MODE включён: доступны демо-учётки и вход без пароля через /api/dev. "
                    + "Не включайте этот режим в промышленной эксплуатации.");
            seedDemo(clinic);
        } else {
            String baseUrl = appProperties.getPublicBaseUrl();
            if (baseUrl == null || !baseUrl.startsWith("https://")
                    || baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
                log.warn("PUBLIC_BASE_URL не похож на публичный HTTPS-адрес: {}. "
                        + "QR-коды и WebApp-кнопки могут не работать.", baseUrl);
            }
            String botToken = appProperties.getBot().getToken();
            String botUsername = appProperties.getBot().getUsername();
            if (botToken == null || botToken.isBlank()
                    || botUsername == null || botUsername.isBlank()) {
                log.warn("Telegram-бот не настроен: привязка пациенток работать не будет.");
            }
        }
    }

    private void seedAdmin(Clinic clinic) {
        String email = appProperties.getAdminEmail();
        String password = appProperties.getAdminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (doctorRepository.existsByEmailIgnoreCase(email.trim())) {
            return;
        }
        Doctor admin = new Doctor();
        admin.setClinicId(clinic.getId());
        admin.setFullName("Администратор");
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setMustChangePassword(true);
        doctorRepository.save(admin);
    }

    private void seedDemo(Clinic clinic) {
        Doctor doctor = doctorRepository.findByEmailIgnoreCase("doctor@example.com")
                .orElseGet(() -> {
                    Doctor created = new Doctor();
                    created.setClinicId(clinic.getId());
                    created.setFullName("Демо Врач");
                    created.setEmail("doctor@example.com");
                    created.setPasswordHash(passwordEncoder.encode("doctor123"));
                    created.setRole(Role.DOCTOR);
                    created.setMustChangePassword(false);
                    return doctorRepository.save(created);
                });

        PatientCard card = patientCardRepository.findByPublicCode("DEMO1234")
                .orElseGet(() -> {
                    PatientCard created = new PatientCard();
                    created.setClinicId(clinic.getId());
                    created.setDoctorId(doctor.getId());
                    created.setPublicCode("DEMO1234");
                    created.setFullName("Иванова Анна Сергеевна");
                    created.setAgeYears(28);
                    created.setParity(1);
                    created.setEdd(LocalDate.now().plusDays(140));
                    created.setHeightCm(BigDecimal.valueOf(165));
                    created.setPrePregnancyWeightKg(BigDecimal.valueOf(62));
                    BigDecimal bmi = BmiCalculator.bmi(created.getHeightCm(), created.getPrePregnancyWeightKg());
                    created.setBmi(bmi);
                    created.setBmiCategory(BmiCalculator.category(bmi));
                    created.setReminderTime(LocalTime.of(9, 0));
                    created.setContentVersion("demo-2026-10");
                    return patientCardRepository.save(created);
                });

        if (patientAccountRepository.findByPatientCardIdAndRevokedAtIsNull(card.getId()).isEmpty()) {
            PatientAccount account = new PatientAccount();
            account.setPatientCardId(card.getId());
            account.setPlatform(Platform.TELEGRAM);
            account.setPlatformUserId("demo-patient");
            account.setConsentVersion(LinkService.CONSENT_VERSION);
            account.setConsentAt(Instant.now());
            patientAccountRepository.save(account);
        }
    }
}
