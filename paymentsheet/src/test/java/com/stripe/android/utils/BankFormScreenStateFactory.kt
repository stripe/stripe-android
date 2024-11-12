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
        bankName: String? = "Stripe Bank",
        last4: String? = "6789",
    ): BankFormScreenState {
        return create(
            resultIdentifier = BankFormScreenState.ResultIdentifier.Session(sessionId),
            isVerifyingWithMicrodeposits = isVerifyingWithMicrodeposits,
            mandateText = mandateText,
            bankName = bankName,
            last4 = last4,
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
            bankName = "Stripe Bank",
            last4 = "6789",
        )
    }

    private fun create(
        resultIdentifier: BankFormScreenState.ResultIdentifier,
        isVerifyingWithMicrodeposits: Boolean,
        mandateText: ResolvableString,
        bankName: String?,
        last4: String?,
    ): BankFormScreenState {
        return BankFormScreenState(
            linkedBankAccount = BankFormScreenState.LinkedBankAccount(
                resultIdentifier = resultIdentifier,
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = bankName,
                last4 = last4,
                mandateText = mandateText,
                isVerifyingWithMicrodeposits = isVerifyingWithMicrodeposits,
            )
        )
    }
}
