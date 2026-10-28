package ru.antenatal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String token,
            String role,
            String fullName,
            Long doctorId,
            boolean mustChangePassword
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {
    }

    public record MeResponse(
            Long id,
            String fullName,
            String email,
            String role,
            Long clinicId
    ) {
    }

    public record PatientSessionRequest(String initData) {
    }

    public record PatientSessionResponse(String token, boolean bound) {
    }
}
