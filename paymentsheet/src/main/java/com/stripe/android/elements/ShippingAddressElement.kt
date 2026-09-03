package com.stripe.android.elements

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.ShippingAddressElementStateHolder
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShippingAddressElement @Inject internal constructor(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val checkoutController: CheckoutController,
    private val stateHolder: CheckoutControllerStateHolder,
    private val shippingAddressElementStateHolder: ShippingAddressElementStateHolder,
    private val errorReporter: ErrorReporter,
) {
    private val activityLauncher: ActivityResultLauncher<AddressElementActivityContract.Args> =
        activityResultCaller.registerForActivityResult(AddressElementActivityContract) { result ->
            shippingAddressElementStateHolder.isPresenting = false
            when (result) {
                is AddressLauncherResult.Succeeded -> {
                    result.address.address?.toCheckoutAddress()?.let { address ->
                        coroutineScope.launch {
                            checkoutController.commitShippingAddress(
                                name = result.address.name,
                                address = address,
                            )
                        }
                    }
                }
                is AddressLauncherResult.Canceled -> Unit
            }
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
        if (stateHolder.state == null) {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.CHECKOUT_SHIPPING_ADDRESS_ELEMENT_PRESENT_NOT_CONFIGURED
            )
            return
        }

        if (shippingAddressElementStateHolder.isPresenting) {
            return
        }

        shippingAddressElementStateHolder.isPresenting = true
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
