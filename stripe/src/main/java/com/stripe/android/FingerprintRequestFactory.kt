package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting

internal class FingerprintRequestFactory @VisibleForTesting internal constructor(
    private val fingerprintRequestParamsFactory: FingerprintRequestParamsFactory,
    private val uidSupplier: Supplier<StripeUid>
) : Factory0<FingerprintRequest> {

    internal constructor(context: Context) : this(
        fingerprintRequestParamsFactory = FingerprintRequestParamsFactory(context),
        uidSupplier = UidSupplier(context)
    )

    override fun create(): FingerprintRequest {
        val guid = uidSupplier.get().value.takeUnless {
            it.isBlank()
        }?.let {
            StripeTextUtils.shaHashInput(it).orEmpty()
        }.orEmpty()

        return FingerprintRequest(
            params = fingerprintRequestParamsFactory.createParams(),
            guid = guid
        )
    }
}
