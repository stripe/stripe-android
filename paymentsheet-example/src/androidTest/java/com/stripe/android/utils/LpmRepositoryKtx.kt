package com.stripe.android.utils

import android.content.Context
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration

internal fun initializedLpmRepository(context: Context): LpmRepository {
    val repository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = context.resources,
        )
    )

    repository.update(
        metadata = PaymentMethodMetadata(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = PaymentMethod.Type.entries.map { it.code },
                paymentMethodOptionsJsonString = """
                {
                    "us_bank_account": {
                        "verification_method": "automatic"
                    }
              }
            """.trimIndent()
            ),
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(),
            allowsDelayedPaymentMethods = true,
            financialConnectionsAvailable = true,
        ),
        serverLpmSpecs = null,
    )

    return repository
}
