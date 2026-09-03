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
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.ShippingAddressUpdater
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShippingAddressElement @Inject internal constructor(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
    private val checkoutController: CheckoutController,
    private val stateHolder: CheckoutControllerStateHolder,
    private val shippingAddressElementStateHolder: ShippingAddressElementStateHolder,
    private val errorReporter: ErrorReporter,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
) {
    private val shippingAddressUpdater: ShippingAddressUpdater = { address ->
        update(address)
    }

    private val activityLauncher: ActivityResultLauncher<AddressElementActivityContract.Args> =
        activityResultCaller.registerForActivityResult(AddressElementActivityContract) {
            unregisterShippingAddressUpdater()
            shippingAddressElementStateHolder.isPresenting = false
        }

    init {
        if (shippingAddressElementStateHolder.isPresenting) {
            registerShippingAddressUpdater()
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    unregisterShippingAddressUpdater()
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

        registerShippingAddressUpdater()
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
                launchMode = AddressElementActivityContract.LaunchMode.CheckoutShipping(
                    paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                ),
            )
        )
    }

    private fun registerShippingAddressUpdater() {
        PaymentElementCallbackReferences.registerShippingAddressUpdater(
            key = paymentElementCallbackIdentifier,
            updater = shippingAddressUpdater,
        )
    }

    private fun unregisterShippingAddressUpdater() {
        PaymentElementCallbackReferences.unregisterShippingAddressUpdater(
            key = paymentElementCallbackIdentifier,
            updater = shippingAddressUpdater,
        )
    }

    private suspend fun update(address: AddressDetails): Result<Unit> {
        val checkoutAddress = runCatching {
            CheckoutController.Address()
                .city(address.address?.city)
                .country(requireNotNull(address.address?.country))
                .line1(address.address?.line1)
                .line2(address.address?.line2)
                .postalCode(address.address?.postalCode)
                .state(address.address?.state)
        }.getOrElse { error ->
            return Result.failure(error)
        }

        return checkoutController.updateShippingAddress(
            name = address.name,
            address = checkoutAddress,
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
