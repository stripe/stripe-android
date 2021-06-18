package com.stripe.android.paymentsheet.paymentdatacollection

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.stripe.android.paymentsheet.forms.FormElementSpec

abstract class BasePaymentDataCollectionFragment : Fragment() {
    abstract fun setProcessing(processing: Boolean)
    abstract fun paramMapLiveData(): LiveData<Map<FormElementSpec.SectionSpec.SectionFieldSpec, String?>?>
}
