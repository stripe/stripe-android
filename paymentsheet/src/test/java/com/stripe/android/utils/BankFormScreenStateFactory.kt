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
    ): BankFormScreenState {
        return create(
            resultIdentifier = BankFormScreenState.ResultIdentifier.Session(sessionId),
            isVerifyingWithMicrodeposits = isVerifyingWithMicrodeposits,
            mandateText = mandateText,
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
        )
    }

    private fun create(
        resultIdentifier: BankFormScreenState.ResultIdentifier,
        isVerifyingWithMicrodeposits: Boolean,
        mandateText: ResolvableString,
    ): BankFormScreenState {
        return BankFormScreenState(
            linkedBankAccount = BankFormScreenState.LinkedBankAccount(
                resultIdentifier = resultIdentifier,
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = "6789",
                primaryButtonText = "Continue".resolvableString,
                mandateText = mandateText,
                isVerifyingWithMicrodeposits = isVerifyingWithMicrodeposits,
            )
        )
    }
}
