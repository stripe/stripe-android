package com.stripe.attestation

import android.content.Context
import androidx.annotation.RestrictTo
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager

/**
 * Factory for creating a [StandardIntegrityManager].
 *
 * This is used to allow for easier testing.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface StandardIntegrityManagerFactory {
    fun create(): StandardIntegrityManager
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealStandardIntegrityManagerFactory(private val appContext: Context) : StandardIntegrityManagerFactory {
    override fun create(): StandardIntegrityManager {
        return IntegrityManagerFactory.createStandard(appContext)
    }
}
