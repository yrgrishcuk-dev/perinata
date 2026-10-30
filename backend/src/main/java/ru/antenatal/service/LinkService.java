package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.common.TokenUtil;
import ru.antenatal.config.AppProperties;
import ru.antenatal.domain.Doctor;
import ru.antenatal.domain.DoctorLinkToken;
import ru.antenatal.domain.DoctorMessenger;
import ru.antenatal.domain.LinkToken;
import ru.antenatal.domain.PatientAccount;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.domain.Platform;
import ru.antenatal.repo.DoctorLinkTokenRepository;
import ru.antenatal.repo.DoctorMessengerRepository;
import ru.antenatal.repo.DoctorRepository;
import ru.antenatal.repo.LinkTokenRepository;
import ru.antenatal.repo.PatientAccountRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.web.dto.DoctorDtos.DoctorMessengerResponse;
import ru.antenatal.web.dto.DoctorDtos.LinkResponse;
import ru.antenatal.web.dto.PatientDtos.InvitePreviewResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LinkService {

    public static final String CONSENT_VERSION = "2026-10-v1";

    public record BotStatus(boolean patient, boolean doctor, String publicCode) {
    }

    public record UnlinkResult(boolean patient, boolean doctor) {
    }

    private final LinkTokenRepository linkTokenRepository;
    private final DoctorLinkTokenRepository doctorLinkTokenRepository;
    private final PatientAccountRepository patientAccountRepository;
    private final PatientCardRepository patientCardRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorMessengerRepository doctorMessengerRepository;
    private final DoctorAccessService doctorAccessService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional
    public LinkResponse createPatientLink(Doctor doctor, Long patientId) {
        PatientCard card = doctorAccessService.requirePatient(doctor, patientId);
        if (card.getStatus() != PatientStatus.ACTIVE) {
            throw ApiException.badRequest("Наблюдение завершено");
        }
        linkTokenRepository.findByPatientCardIdAndUsedAtIsNullAndRevokedAtIsNull(card.getId())
                .forEach(old -> {
                    old.setRevokedAt(Instant.now());
                    linkTokenRepository.save(old);
                });

        String raw = TokenUtil.randomToken(16);
        Instant expiresAt = Instant.now().plus(appProperties.getLinkTokenTtlMinutes(), ChronoUnit.MINUTES);
        LinkToken token = new LinkToken();
        token.setTokenHash(TokenUtil.sha256(raw));
        token.setPatientCardId(card.getId());
        token.setCreatedBy(doctor.getId());
        token.setExpiresAt(expiresAt);
        linkTokenRepository.save(token);
        auditService.log(doctor.getRole().name(), String.valueOf(doctor.getId()),
                "LINK_CREATE", "PATIENT_CARD", String.valueOf(card.getId()), null);
        return new LinkResponse(appProperties.getPublicBaseUrl() + "/l/" + raw, expiresAt);
    }

    @Transactional
    public DoctorMessengerResponse createDoctorLink(Doctor doctor, Platform platform) {
        String username;
        String baseUrl;
        if (platform == Platform.MAX) {
            username = appProperties.getMax().getUsername();
            baseUrl = "https://max.ru/";
            if (username == null || username.isBlank()) {
                throw ApiException.badRequest("Не задан MAX_BOT_USERNAME");
            }
        } else {
            username = appProperties.getBot().getUsername();
            baseUrl = "https://t.me/";
            if (username == null || username.isBlank()) {
                throw ApiException.badRequest("Не задан TELEGRAM_BOT_USERNAME");
            }
        }
        doctorLinkTokenRepository.findByDoctorIdAndUsedAtIsNull(doctor.getId())
                .forEach(old -> {
                    old.setUsedAt(Instant.now());
                    doctorLinkTokenRepository.save(old);
                });

        String raw = TokenUtil.randomToken(16);
        DoctorLinkToken token = new DoctorLinkToken();
        token.setTokenHash(TokenUtil.sha256(raw));
        token.setDoctorId(doctor.getId());
        token.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        doctorLinkTokenRepository.save(token);

        boolean linked = doctorMessengerRepository
                .findByDoctorIdAndRevokedAtIsNull(doctor.getId()).isPresent();
        String url = baseUrl + username + "?start=dr_" + raw;
        return new DoctorMessengerResponse(url, linked);
    }

    @Transactional(readOnly = true)
    public InvitePreviewResponse preview(String rawToken, Platform platform, String platformUserId) {
        LinkToken token = requireValidToken(rawToken);
        PatientCard card = patientCardRepository.findById(token.getPatientCardId())
                .orElseThrow(() -> ApiException.notFound("Карточка не найдена"));
        Doctor doctor = doctorRepository.findById(card.getDoctorId()).orElse(null);

        PatientAccount existing = patientAccountRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(platform, platformUserId)
                .orElse(null);
        boolean boundToThis = existing != null && existing.getPatientCardId().equals(card.getId());
        boolean boundToOther = existing != null && !existing.getPatientCardId().equals(card.getId());

        return new InvitePreviewResponse(
                card.getFullName(),
                card.getAgeYears(),
                card.getEdd(),
                doctor == null ? null : doctor.getFullName(),
                boundToOther,
                boundToThis);
    }

    @Transactional
    public PatientCard confirm(String rawToken, Platform platform, String platformUserId,
                               String chatId, boolean consent) {
        if (!consent) {
            throw ApiException.badRequest("Нужно согласие на обработку данных");
        }
        LinkToken token = requireValidToken(rawToken);
        PatientCard card = patientCardRepository.findById(token.getPatientCardId())
                .orElseThrow(() -> ApiException.notFound("Карточка не найдена"));
        if (card.getStatus() != PatientStatus.ACTIVE) {
            throw ApiException.badRequest("Наблюдение завершено");
        }

        Optional<PatientAccount> byMessenger = patientAccountRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(platform, platformUserId);
        if (byMessenger.isPresent()) {
            if (byMessenger.get().getPatientCardId().equals(card.getId())) {
                markUsed(token);
                return card;
            }
            throw ApiException.conflict("Этот аккаунт уже привязан к другой карточке");
        }

        Optional<PatientAccount> byCard = patientAccountRepository
                .findByPatientCardIdAndRevokedAtIsNull(card.getId());
        if (byCard.isPresent()) {
            throw ApiException.conflict("Карточка уже привязана к другому аккаунту");
        }

        PatientAccount account = new PatientAccount();
        account.setPatientCardId(card.getId());
        account.setPlatform(platform);
        account.setPlatformUserId(platformUserId);
        account.setChatId(chatId);
        account.setConsentVersion(CONSENT_VERSION);
        account.setConsentAt(Instant.now());
        patientAccountRepository.save(account);
        markUsed(token);

        auditService.log("PATIENT", platformUserId, "BINDING_CONFIRM",
                "PATIENT_CARD", String.valueOf(card.getId()),
                "consent=" + CONSENT_VERSION + ",platform=" + platform);
        return card;
    }

    @Transactional
    public Doctor confirmDoctorLink(String rawToken, Platform platform, String chatId,
                                    String platformUserId) {
        DoctorLinkToken token = doctorLinkTokenRepository.findByTokenHash(TokenUtil.sha256(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("Ссылка недействительна"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("Ссылка истекла");
        }
        Doctor doctor = doctorRepository.findById(token.getDoctorId())
                .orElseThrow(() -> ApiException.notFound("Врач не найден"));

        doctorMessengerRepository.findByDoctorIdAndRevokedAtIsNull(doctor.getId())
                .ifPresent(existing -> {
                    existing.setRevokedAt(Instant.now());
                    doctorMessengerRepository.save(existing);
                });

        DoctorMessenger messenger = new DoctorMessenger();
        messenger.setDoctorId(doctor.getId());
        messenger.setPlatform(platform);
        messenger.setPlatformUserId(platformUserId);
        messenger.setChatId(chatId);
        doctorMessengerRepository.save(messenger);

        token.setUsedAt(Instant.now());
        doctorLinkTokenRepository.save(token);
        auditService.log("DOCTOR", String.valueOf(doctor.getId()),
                "MESSENGER_LINK", "DOCTOR", String.valueOf(doctor.getId()),
                "platform=" + platform);
        return doctor;
    }

    @Transactional(readOnly = true)
    public BotStatus botStatus(Platform platform, String platformUserId) {
        Optional<PatientAccount> patient = patientAccountRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(platform, platformUserId);
        Optional<DoctorMessenger> doctor = doctorMessengerRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(platform, platformUserId);
        String publicCode = patient
                .flatMap(account -> patientCardRepository.findById(account.getPatientCardId()))
                .map(PatientCard::getPublicCode)
                .orElse(null);
        return new BotStatus(patient.isPresent(), doctor.isPresent(), publicCode);
    }

    @Transactional
    public UnlinkResult unlinkByMessenger(Platform platform, String platformUserId) {
        boolean patient = false;
        boolean doctor = false;

        Optional<PatientAccount> account = patientAccountRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(platform, platformUserId);
        if (account.isPresent()) {
            PatientAccount value = account.get();
            value.setRevokedAt(Instant.now());
            patientAccountRepository.save(value);
            revokeUnusedLinkTokens(value.getPatientCardId());
            auditService.log("PATIENT", platformUserId, "BOT_UNLINK",
                    "PATIENT_CARD", String.valueOf(value.getPatientCardId()),
                    "platform=" + platform);
            patient = true;
        }

        Optional<DoctorMessenger> messenger = doctorMessengerRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(platform, platformUserId);
        if (messenger.isPresent()) {
            DoctorMessenger value = messenger.get();
            value.setRevokedAt(Instant.now());
            doctorMessengerRepository.save(value);
            auditService.log("DOCTOR", String.valueOf(value.getDoctorId()), "BOT_UNLINK",
                    "DOCTOR", String.valueOf(value.getDoctorId()), "platform=" + platform);
            doctor = true;
        }

        return new UnlinkResult(patient, doctor);
    }

    @Transactional(readOnly = true)
    public Optional<LinkToken> findValid(String rawToken) {
        return linkTokenRepository.findByTokenHash(TokenUtil.sha256(rawToken))
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getRevokedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()));
    }

    private LinkToken requireValidToken(String rawToken) {
        return findValid(rawToken)
                .orElseThrow(() -> ApiException.unauthorized("Ссылка недействительна или истекла"));
    }

    private void markUsed(LinkToken token) {
        token.setUsedAt(Instant.now());
        linkTokenRepository.save(token);
    }

    private void revokeUnusedLinkTokens(Long patientCardId) {
        linkTokenRepository.findByPatientCardIdAndUsedAtIsNullAndRevokedAtIsNull(patientCardId)
                .forEach(token -> {
                    token.setRevokedAt(Instant.now());
                    linkTokenRepository.save(token);
                });
    }
}
