package com.stripe.android.paymentsheet.viewmodels

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PaymentOptionsItemsMapper(
    private val customerMetadata: StateFlow<CustomerMetadata?>,
    private val customerState: StateFlow<CustomerState?>,
    private val isGooglePayReady: StateFlow<Boolean>,
    private val isLinkEnabled: StateFlow<Boolean?>,
    private val nameProvider: (PaymentMethodCode?) -> ResolvableString,
    private val isNotPaymentFlow: Boolean,
    private val isCbcEligible: () -> Boolean
) {

    operator fun invoke(): StateFlow<List<PaymentOptionsItem>> {
        return combineAsStateFlow(
            customerState,
            isLinkEnabled,
            isGooglePayReady,
            customerMetadata,
        ) { customerState, isLinkEnabled, isGooglePayReady, customerMetadata ->
            createPaymentOptionsItems(
                paymentMethods = customerState?.paymentMethods ?: listOf(),
                isLinkEnabled = isLinkEnabled,
                isGooglePayReady = isGooglePayReady,
                defaultPaymentMethodId = if (customerMetadata?.isPaymentMethodSetAsDefaultEnabled == true) {
                    customerState?.defaultPaymentMethodId
                } else {
                    null
                }
            ) ?: emptyList()
        }
    }

    @Suppress("ReturnCount")
    private fun createPaymentOptionsItems(
        paymentMethods: List<PaymentMethod>,
        isLinkEnabled: Boolean?,
        isGooglePayReady: Boolean,
        defaultPaymentMethodId: String?
    ): List<PaymentOptionsItem>? {
        if (isLinkEnabled == null) return null

        return PaymentOptionsStateFactory.createPaymentOptionsList(
            paymentMethods = paymentMethods,
            showGooglePay = isGooglePayReady && isNotPaymentFlow,
            showLink = isLinkEnabled && isNotPaymentFlow,
            nameProvider = nameProvider,
            isCbcEligible = isCbcEligible(),
            defaultPaymentMethodId = defaultPaymentMethodId
        )
    }
}
