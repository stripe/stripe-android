package com.stripe.android.paymentsheet.flowcontroller

import androidx.lifecycle.ViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FlowControllerViewModel : ViewModel() {
    var initData: InitData? = null
    var paymentSelection: PaymentSelection? = null
}
