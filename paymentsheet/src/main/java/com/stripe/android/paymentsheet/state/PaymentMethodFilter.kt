package com.stripe.android.paymentsheet.state

import com.stripe.android.common.validation.isSupportedWithBillingConfig
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.CardFunding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.state.PaymentMethodFilter.FilterParams
import kotlinx.coroutines.Deferred
import javax.inject.Inject

internal interface PaymentMethodFilter {
    suspend fun filter(
        paymentMethods: List<PaymentMethod>,
        params: FilterParams,
    ): List<PaymentMethod>

    class FilterParams(
        val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        val customerMetadata: CustomerMetadata?,
        val remoteDefaultPaymentMethodId: String?,
        val cardBrandFilter: PaymentSheetCardBrandFilter,
        val cardFundingFilter: PaymentSheetCardFundingFilter,
        val localSavedSelection: Deferred<SavedSelection>,
    )
}

internal class DefaultPaymentMethodFilter @Inject constructor() : PaymentMethodFilter {
    override suspend fun filter(
        paymentMethods: List<PaymentMethod>,
        params: FilterParams,
    ): List<PaymentMethod> {
        return paymentMethods.withDefaultPaymentMethodOrLastUsedPaymentMethodFirst(
            savedSelection = params.localSavedSelection,
            defaultPaymentMethodId = params.remoteDefaultPaymentMethodId,
            isPaymentMethodSetAsDefaultEnabled = params.customerMetadata?.isPaymentMethodSetAsDefaultEnabled
                ?: IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
        ).filter { paymentMethod ->
            val fundingAccepted = paymentMethod.card?.let {
                params.cardFundingFilter.isAccepted(CardFunding.fromCode(it.funding))
            } ?: true
            params.cardBrandFilter.isAccepted(paymentMethod) &&
                fundingAccepted &&
                paymentMethod.isSupportedWithBillingConfig(params.billingDetailsCollectionConfiguration)
        }
    }
}

private suspend fun List<PaymentMethod>.withDefaultPaymentMethodOrLastUsedPaymentMethodFirst(
    savedSelection: Deferred<SavedSelection>?,
    isPaymentMethodSetAsDefaultEnabled: Boolean,
    defaultPaymentMethodId: String?,
): List<PaymentMethod> {
    val primaryPaymentMethodId = if (isPaymentMethodSetAsDefaultEnabled) {
        defaultPaymentMethodId
    } else {
        val paymentMethodSelection = savedSelection?.await() as? SavedSelection.PaymentMethod
        paymentMethodSelection?.id
    }

    val primaryPaymentMethod = this.firstOrNull { it.id == primaryPaymentMethodId }

    return primaryPaymentMethod?.let {
        listOf(primaryPaymentMethod) + (this - primaryPaymentMethod)
    } ?: this
}
