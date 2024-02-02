package com.stripe.android.utils

import android.content.Context
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentIntentFactory

internal fun initializedLpmRepository(context: Context): LpmRepository {
    val repository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = context.resources,
            isFinancialConnectionsAvailable = { true }
        )
    )

    repository.update(
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
        serverLpmSpecs = null,
    )

    return repository
}
