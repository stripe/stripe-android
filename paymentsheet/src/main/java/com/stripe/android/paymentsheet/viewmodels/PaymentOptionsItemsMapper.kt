package com.stripe.android.paymentsheet.viewmodels

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PaymentOptionsItemsMapper(
    private val paymentMethods: StateFlow<List<PaymentMethod>>,
    private val googlePayState: StateFlow<GooglePayState>,
    private val isLinkEnabled: StateFlow<Boolean?>,
    private val nameProvider: (PaymentMethodCode?) -> String,
    private val isNotPaymentFlow: Boolean,
    private val isCbcEligible: () -> Boolean
) {

    operator fun invoke(): StateFlow<List<PaymentOptionsItem>> {
        return combineAsStateFlow(
            paymentMethods,
            isLinkEnabled,
            googlePayState,
        ) { paymentMethods, isLinkEnabled, googlePayState ->
            createPaymentOptionsItems(
                paymentMethods = paymentMethods,
                isLinkEnabled = isLinkEnabled,
                // TODO(samer-stripe): Set this based on customer_session permissions
                canRemovePaymentMethods = true,
                googlePayState = googlePayState,
            ) ?: emptyList()
        }
    }

    @Suppress("ReturnCount")
    private fun createPaymentOptionsItems(
        paymentMethods: List<PaymentMethod>,
        isLinkEnabled: Boolean?,
        canRemovePaymentMethods: Boolean?,
        googlePayState: GooglePayState,
    ): List<PaymentOptionsItem>? {
        if (isLinkEnabled == null) return null

        return PaymentOptionsStateFactory.createPaymentOptionsList(
            paymentMethods = paymentMethods,
            showGooglePay = (googlePayState is GooglePayState.Available) && isNotPaymentFlow,
            showLink = isLinkEnabled && isNotPaymentFlow,
            nameProvider = nameProvider,
            isCbcEligible = isCbcEligible(),
            canRemovePaymentMethods = canRemovePaymentMethods ?: false
        )
    }
}
