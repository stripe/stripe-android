package com.stripe.android.elements

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import dagger.Lazy
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShippingAddressElement @Inject internal constructor(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val paymentConfiguration: Lazy<PaymentConfiguration>,
    private val stateHolder: CheckoutControllerStateHolder,
) {
    private var isPresenting = false

    private val activityLauncher: ActivityResultLauncher<AddressElementActivityContract.Args> =
        activityResultCaller.registerForActivityResult(AddressElementActivityContract) {
            isPresenting = false
        }

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityLauncher.unregister()
                    super.onDestroy(owner)
                }
            }
        )
    }

    fun present() {
        if (stateHolder.state == null || isPresenting) {
            return
        }

        isPresenting = true
        activityLauncher.launch(
            AddressElementActivityContract.Args(
                publishableKey = paymentConfiguration.get().publishableKey,
                config = AddressLauncher.Configuration(
                    additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                        phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                    ),
                    billingAddress = null,
                    useStripeHostedAutocomplete = true,
                ),
            )
        )
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var googlePlacesApiKey: String? = null

        fun googlePlacesApiKey(googlePlacesApiKey: String?): Configuration = apply {
            this.googlePlacesApiKey = googlePlacesApiKey
        }

        @Parcelize
        internal data class State(
            val googlePlacesApiKey: String?,
        ) : Parcelable

        internal fun build(): State = State(
            googlePlacesApiKey = googlePlacesApiKey,
        )
    }
}
