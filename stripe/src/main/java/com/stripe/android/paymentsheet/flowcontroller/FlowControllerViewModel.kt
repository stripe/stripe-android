package com.stripe.android.paymentsheet.flowcontroller

import androidx.lifecycle.ViewModel
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FlowControllerViewModel : ViewModel() {
    private var _initData: InitData? = null

    var paymentSelection: PaymentSelection? = null
    val newlySavedPaymentMethods = mutableListOf<PaymentMethod>()

    fun setInitData(initData: InitData) {
        _initData = initData
    }

    val initData: InitData get() = requireNotNull(_initData)
}
