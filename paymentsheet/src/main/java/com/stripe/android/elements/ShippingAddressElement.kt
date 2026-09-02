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
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.CheckoutShippingAddressUpdaterRegistry
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
) : CheckoutShippingAddressUpdaterRegistry.Updater {
    private val activityLauncher: ActivityResultLauncher<AddressElementActivityContract.Args> =
        activityResultCaller.registerForActivityResult(AddressElementActivityContract) {
            CheckoutShippingAddressUpdaterRegistry.remove(shippingAddressElementStateHolder.updaterKey)
            shippingAddressElementStateHolder.updaterKey = null
            shippingAddressElementStateHolder.isPresenting = false
        }

    init {
        if (shippingAddressElementStateHolder.isPresenting) {
            shippingAddressElementStateHolder.updaterKey?.let { updaterKey ->
                CheckoutShippingAddressUpdaterRegistry.register(updaterKey, this)
            }
        }

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

        val updaterKey = CheckoutShippingAddressUpdaterRegistry.register(this)
        shippingAddressElementStateHolder.updaterKey = updaterKey
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
                updaterKey = updaterKey,
            )
        )
    }

    override suspend fun update(address: AddressDetails): Result<Unit> {
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
