package com.stripe.android.paymentsheet.paymentdatacollection

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData

abstract class BasePaymentDataCollectionFragment : Fragment() {
    abstract fun setProcessing(processing: Boolean)
    abstract fun paramMapLiveData(): LiveData<Map<String, Any?>?>
}
