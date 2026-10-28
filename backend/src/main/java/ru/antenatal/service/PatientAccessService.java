package ru.antenatal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antenatal.common.ApiException;
import ru.antenatal.domain.PatientAccount;
import ru.antenatal.domain.PatientCard;
import ru.antenatal.domain.PatientStatus;
import ru.antenatal.domain.Platform;
import ru.antenatal.repo.PatientAccountRepository;
import ru.antenatal.repo.PatientCardRepository;
import ru.antenatal.security.CurrentPrincipal;

@Service
@RequiredArgsConstructor
public class PatientAccessService {

    private final PatientAccountRepository patientAccountRepository;
    private final PatientCardRepository patientCardRepository;

    public record PatientContext(PatientCard card, PatientAccount account) {
    }

    @Transactional(readOnly = true)
    public PatientContext requireContext(CurrentPrincipal principal) {
        String telegramId = principal.requireTelegramId();
        PatientAccount account = patientAccountRepository
                .findByPlatformAndPlatformUserIdAndRevokedAtIsNull(Platform.TELEGRAM, telegramId)
                .orElseThrow(() -> ApiException.conflict("Карточка ещё не привязана"));
        PatientCard card = patientCardRepository.findById(account.getPatientCardId())
                .orElseThrow(() -> ApiException.notFound("Карточка не найдена"));
        if (card.getStatus() != PatientStatus.ACTIVE) {
            throw ApiException.forbidden("Наблюдение завершено");
        }
        return new PatientContext(card, account);
    }

    @Transactional(readOnly = true)
    public PatientCard requireCard(CurrentPrincipal principal) {
        return requireContext(principal).card();
    }

    @Transactional
    public void revokeConsent(PatientAccount account) {
        account.setRevokedAt(java.time.Instant.now());
        patientAccountRepository.save(account);
    }
}
