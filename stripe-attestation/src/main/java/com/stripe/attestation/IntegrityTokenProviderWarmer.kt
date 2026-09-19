package com.stripe.attestation

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IntegrityTokenProviderWarmer {
    suspend fun warmup(): Result<Unit>
}
