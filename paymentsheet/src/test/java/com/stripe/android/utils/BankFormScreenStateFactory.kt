package com.stripe.android.utils

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.ach.BankFormScreenState

internal object BankFormScreenStateFactory {

    fun createWithSession(
        sessionId: String,
        isVerifyingWithMicrodeposits: Boolean = false,
        mandateText: ResolvableString = "Some legal text".resolvableString,
        promoText: String? = null,
        eligibleForPromo: Boolean = true,
        bankName: String = "Stripe Bank",
    ): BankFormScreenState {
        return create(
            resultIdentifier = BankFormScreenState.ResultIdentifier.Session(sessionId),
            isVerifyingWithMicrodeposits = isVerifyingWithMicrodeposits,
            mandateText = mandateText,
            promoText = promoText,
            eligibleForPromo = eligibleForPromo,
            bankName = bankName,
        )
    }

    fun createWithPaymentMethod(
        paymentMethod: PaymentMethod,
        isVerifyingWithMicrodeposits: Boolean = false,
        mandateText: ResolvableString = "Some legal text".resolvableString,
    ): BankFormScreenState {
        return create(
            resultIdentifier = BankFormScreenState.ResultIdentifier.PaymentMethod(paymentMethod),
            isVerifyingWithMicrodeposits = isVerifyingWithMicrodeposits,
            mandateText = mandateText,
            promoText = null,
            eligibleForPromo = false,
        )
    }

    private fun create(
        resultIdentifier: BankFormScreenState.ResultIdentifier,
        isVerifyingWithMicrodeposits: Boolean,
        mandateText: ResolvableString,
        promoText: String?,
        eligibleForPromo: Boolean,
        bankName: String = "Stripe Bank",
    ): BankFormScreenState {
        return BankFormScreenState(
            isPaymentFlow = true,
            linkedBankAccount = BankFormScreenState.LinkedBankAccount(
                resultIdentifier = resultIdentifier,
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = bankName,
                last4 = "6789",
                mandateText = mandateText,
                isVerifyingWithMicrodeposits = isVerifyingWithMicrodeposits,
                eligibleForIncentive = eligibleForPromo,
            ),
            promoText = promoText,
        )
    }
}
