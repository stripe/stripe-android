package com.stripe.android.lpmfoundations.paymentmethod

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object PaymentMethodMetadataFactory {
    fun create(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration(),
        allowsDelayedPaymentMethods: Boolean = true,
        allowsPaymentMethodsRequiringShippingAddress: Boolean = false,
        financialConnectionsAvailable: Boolean = true,
        sharedDataSpecs: List<SharedDataSpec> = createSharedDataSpecs(),
    ): PaymentMethodMetadata {
        return PaymentMethodMetadata(
            stripeIntent = stripeIntent,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
            allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
            financialConnectionsAvailable = financialConnectionsAvailable,
            sharedDataSpecs = sharedDataSpecs,
        )
    }

    private fun createSharedDataSpecs(): List<SharedDataSpec> = runCatching {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val specsString = resources.assets!!.open("lpms.json").bufferedReader().use { it.readText() }
        LpmSerializer.deserializeList(specsString).getOrThrow()
    }.getOrElse {
        createSharedDataSpecsWithCardOnly()
    }

    fun createSharedDataSpecsWithCardOnly(): List<SharedDataSpec> {
        return listOf(SharedDataSpec(type = "card", fields = ArrayList()))
    }
}
