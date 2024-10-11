package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal.InstantDebitsData
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal.USBankAccountData
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry

internal fun CollectBankAccountResultInternal.toFormFieldValues(): List<Pair<IdentifierSpec, FormFieldEntry>> {
    return when (this) {
        is CollectBankAccountResultInternal.Completed -> {
            response.usBankAccountData?.toFormFieldValues()
                ?: response.instantDebitsData?.toFormFieldValues()
                ?: emptyList()
        }
        is CollectBankAccountResultInternal.Cancelled -> {
            emptyList()
        }
        is CollectBankAccountResultInternal.Failed -> {
            emptyList()
        }
    }
}

private fun USBankAccountData.toFormFieldValues(): List<Pair<IdentifierSpec, FormFieldEntry>> {
    val bankName = when (val paymentAccount = financialConnectionsSession.paymentAccount) {
        is BankAccount -> paymentAccount.bankName
        is FinancialConnectionsAccount -> paymentAccount.institutionName
        null -> null
    }

    val last4 = when (val paymentAccount = financialConnectionsSession.paymentAccount) {
        is BankAccount -> paymentAccount.last4
        is FinancialConnectionsAccount -> paymentAccount.last4
        null -> null
    }

    val usesMicrodeposits = when (financialConnectionsSession.paymentAccount) {
        is BankAccount -> true
        is FinancialConnectionsAccount -> false
        null -> false
    }

    return listOf(
        IdentifierSpec.LinkAccountSessionId to completeField(financialConnectionsSession.id),
        IdentifierSpec.BankName to completeField(bankName),
        IdentifierSpec.Last4 to completeField(last4),
        IdentifierSpec.UsesMicrodeposits to completeField(usesMicrodeposits.toString()),
    )
}

private fun InstantDebitsData.toFormFieldValues(): List<Pair<IdentifierSpec, FormFieldEntry>> {
    return listOf(
        IdentifierSpec.LinkPaymentMethodId to completeField(paymentMethodId),
        IdentifierSpec.BankName to completeField(bankName),
        IdentifierSpec.Last4 to completeField(last4),
    )
}

private fun completeField(value: String?): FormFieldEntry {
    return FormFieldEntry(value, isComplete = true)
}
