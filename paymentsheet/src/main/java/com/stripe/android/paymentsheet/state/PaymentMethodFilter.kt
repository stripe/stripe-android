package com.stripe.android.paymentsheet.state

import com.stripe.android.common.validation.isSupportedWithBillingConfig
import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.CardFunding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.state.PaymentMethodFilter.FilterParams
import javax.inject.Inject

internal interface PaymentMethodFilter {
    fun filter(
        paymentMethods: List<PaymentMethod>,
        params: FilterParams,
    ): List<PaymentMethod>

    class FilterParams(
        val metadata: PaymentMethodMetadata,
        val remoteDefaultPaymentMethodId: String?,
        val cardBrandFilter: PaymentSheetCardBrandFilter,
        val cardFundingFilter: PaymentSheetCardFundingFilter,
        val localSavedSelection: SavedSelection.PaymentMethod?,
    )
}

internal class DefaultPaymentMethodFilter @Inject constructor() : PaymentMethodFilter {
    override fun filter(
        paymentMethods: List<PaymentMethod>,
        params: FilterParams,
    ): List<PaymentMethod> {
        return paymentMethods.withDefaultPaymentMethodOrLastUsedPaymentMethodFirst(
            savedSelection = params.localSavedSelection,
            defaultPaymentMethodId = params.remoteDefaultPaymentMethodId,
            isPaymentMethodSetAsDefaultEnabled = params.metadata.customerMetadata
                ?.isPaymentMethodSetAsDefaultEnabled
                ?: IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
        ).filter { paymentMethod ->
            val fundingAccepted = paymentMethod.card?.let {
                params.cardFundingFilter.isAccepted(CardFunding.fromCode(it.funding))
            } ?: true
            params.cardBrandFilter.isAccepted(paymentMethod) &&
                fundingAccepted &&
                paymentMethod.isSupportedWithBillingConfig(
                    params.metadata.billingDetailsCollectionConfiguration
                )
        }
    }
}

private fun List<PaymentMethod>.withDefaultPaymentMethodOrLastUsedPaymentMethodFirst(
    savedSelection: SavedSelection.PaymentMethod?,
    isPaymentMethodSetAsDefaultEnabled: Boolean,
    defaultPaymentMethodId: String?,
): List<PaymentMethod> {
    val primaryPaymentMethodId = if (isPaymentMethodSetAsDefaultEnabled) {
        defaultPaymentMethodId
    } else {
        savedSelection?.id
    }

    val primaryPaymentMethod = this.firstOrNull { it.id == primaryPaymentMethodId }

    return primaryPaymentMethod?.let {
        listOf(primaryPaymentMethod) + (this - primaryPaymentMethod)
    } ?: this
}
