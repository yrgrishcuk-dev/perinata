package ru.antenatal.security;

import ru.antenatal.common.ApiException;
import ru.antenatal.domain.PrincipalType;

public record CurrentPrincipal(PrincipalType type, Long principalId, String externalId) {

    public boolean isDoctor() {
        return type == PrincipalType.DOCTOR;
    }

    public boolean isAdmin() {
        return type == PrincipalType.ADMIN;
    }

    public boolean isTelegram() {
        return type == PrincipalType.TELEGRAM;
    }

    public boolean isMax() {
        return type == PrincipalType.MAX;
    }

    public Long requireDoctorId() {
        if (principalId == null || (type != PrincipalType.DOCTOR && type != PrincipalType.ADMIN)) {
            throw ApiException.forbidden("Требуется вход врача");
        }
        return principalId;
    }

    public Long requireAdminId() {
        if (principalId == null || type != PrincipalType.ADMIN) {
            throw ApiException.forbidden("Требуется вход администратора");
        }
        return principalId;
    }

    public String requireTelegramId() {
        if (externalId == null || type != PrincipalType.TELEGRAM) {
            throw ApiException.forbidden("Требуется вход через Telegram");
        }
        return externalId;
    }

    public String requireMessengerId() {
        if (externalId == null || (type != PrincipalType.TELEGRAM && type != PrincipalType.MAX)) {
            throw ApiException.forbidden("Требуется вход через мессенджер");
        }
        return externalId;
    }
}
