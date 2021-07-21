package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.stripe.android.paymentsheet.AddressFieldRepository
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FlowControllerViewModel : AndroidViewModel() {
    private var _initData: InitData? = null

    var paymentSelection: PaymentSelection? = null

    fun setInitData(initData: InitData) {
        _initData = initData
    }

    fun initializeAddressRepository() {
        AddressFieldRepository.INSTANCE.init(
            getApplication<Application>().baseContext
        )
    }

    val initData: InitData get() = requireNotNull(_initData)
}
