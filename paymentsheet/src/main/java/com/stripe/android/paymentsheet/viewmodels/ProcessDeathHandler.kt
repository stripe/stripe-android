package com.stripe.android.paymentsheet.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.New.Card
import com.stripe.android.paymentsheet.model.PaymentSelection.New.GenericPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection.New.LinkInline
import com.stripe.android.paymentsheet.model.PaymentSelection.New.USBankAccount
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.flow.StateFlow

private const val KeyFormFieldValues = "FormFieldValues"
private const val KeyNewPaymentMethodSelection = "NewPaymentMethodSelection"

internal class ProcessDeathHandler(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
) {

    val newPaymentMethodSelection: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = KeyNewPaymentMethodSelection,
        initialValue = null,
    )

    var pendingPaymentResult: InternalPaymentResult? = null
        private set

    fun handlePaymentMethodTypeSelected(code: String): Boolean {
        val previousCode = savedStateHandle.getAndSet(KeyNewPaymentMethodSelection, code)
        return previousCode != code
    }

    fun handlePaymentLauncherResult(result: InternalPaymentResult) {
        pendingPaymentResult = result
    }

    fun restorePaymentSelection(
        supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
        restoredPaymentSelection: PaymentSelection.New?,
    ): PaymentSelection.New? {
        val firstSelection = savedStateHandle.setAndGet(KeyNewPaymentMethodSelection) {
            when (restoredPaymentSelection) {
                is LinkInline -> {
                    PaymentMethod.Type.Card.code
                }
                is Card, is USBankAccount, is GenericPaymentMethod -> {
                    restoredPaymentSelection.paymentMethodCreateParams.typeCode
                }
                null -> {
                    savedStateHandle[KeyNewPaymentMethodSelection] ?: supportedPaymentMethods.first().code
                }
            }
        }

        val formFieldValues = savedStateHandle.get<FormFieldValues>(KeyFormFieldValues)
        val paymentMethod = supportedPaymentMethods.firstOrNull { it.code == firstSelection }

        val paymentSelection = paymentMethod?.let {
            formFieldValues?.transformToPaymentSelection(
                resources = context.resources,
                paymentMethod = it,
            )
        }

        return paymentSelection
    }
}

private fun <T> SavedStateHandle.getAndSet(key: String, value: T?): T? {
    val current = get<T>(key)
    set(key, value)
    return current
}

private fun <T> SavedStateHandle.setAndGet(key: String, valueProducer: () -> T?): T? {
    val newValue = valueProducer()
    set(key, valueProducer())
    return newValue
}
