package com.stripe.android.paymentsheet.paymentdatacollection

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData

/**
 * This is a temporary base class to support the new data collection forms and also the legacy
 * CardDataCollectionFragment. Once CardDataCollectionFragment is migrated to Compose, it won't be
 * needed anymore.
 */
abstract class BasePaymentDataCollectionFragment : Fragment() {
    /**
     * Inform the fragment whether PaymentSheet is in a processing state, so the fragment knows it
     * should show as enabled or disabled.
     */
    abstract fun setProcessing(processing: Boolean)

    /**
     * Provide to PaymentSheet a LiveData of the map to be used to create the payment method through
     * PaymentMethodCreateParams. If the form is currently invalid, the map should be null.
     */
    abstract fun paramMapLiveData(): LiveData<Map<String, Any?>?>
}
