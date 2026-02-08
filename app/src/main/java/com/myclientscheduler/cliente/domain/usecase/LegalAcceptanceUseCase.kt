package com.myclientscheduler.cliente.domain.usecase

import java.time.Instant

data class LegalAcceptance(val termsVersion: String, val privacyVersion: String, val acceptedAt: Instant)

class LegalAcceptanceUseCase {
    fun accept(termsVersion: String, privacyVersion: String): LegalAcceptance {
        require(termsVersion.isNotBlank() && privacyVersion.isNotBlank())
        return LegalAcceptance(termsVersion, privacyVersion, Instant.now())
    }
}
