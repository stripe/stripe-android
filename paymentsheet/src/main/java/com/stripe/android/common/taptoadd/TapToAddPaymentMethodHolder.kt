package com.stripe.android.common.taptoadd

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethod
import javax.inject.Inject

internal interface TapToAddPaymentMethodHolder {
    val collectedPaymentMethod: PaymentMethod?

    fun setCollectedPaymentMethod(paymentMethod: PaymentMethod)

    companion object {
        fun create(savedStateHandle: SavedStateHandle): TapToAddPaymentMethodHolder {
            return DefaultTapToAddPaymentMethodHolder(savedStateHandle)
        }
    }
}

internal class DefaultTapToAddPaymentMethodHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : TapToAddPaymentMethodHolder {
    private var _collectedPaymentMethod: PaymentMethod?
        get() = savedStateHandle[STORED_TTA_COLLECTED_PAYMENT_METHOD_KEY]
        set(value) {
            savedStateHandle[STORED_TTA_COLLECTED_PAYMENT_METHOD_KEY] = value
        }

    override val collectedPaymentMethod: PaymentMethod?
        get() = _collectedPaymentMethod

    override fun setCollectedPaymentMethod(paymentMethod: PaymentMethod) {
        _collectedPaymentMethod = paymentMethod
    }

    private companion object {
        const val STORED_TTA_COLLECTED_PAYMENT_METHOD_KEY = "STORED_TTA_COLLECTED_PAYMENT_METHOD"
    }
}
