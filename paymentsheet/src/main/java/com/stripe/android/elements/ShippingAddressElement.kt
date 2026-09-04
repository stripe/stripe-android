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
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider

@OptIn(CheckoutSessionPreview::class)
internal fun interface CommitShippingAddress {
    suspend operator fun invoke(
        updatedCheckoutSessionResponse: CheckoutSessionResponse,
        name: String?,
        address: CheckoutController.Address.State,
    ): Result<Unit>
}

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShippingAddressElement internal constructor(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val commitShippingAddress: CommitShippingAddress,
    private val stateHolder: CheckoutControllerStateHolder,
    private val shippingAddressElementStateHolder: ShippingAddressElementStateHolder,
    private val errorReporter: ErrorReporter,
) {
    @Inject
    internal constructor(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        paymentConfiguration: Provider<PaymentConfiguration>,
        @ViewModelScope coroutineScope: CoroutineScope,
        checkoutController: CheckoutController,
        stateHolder: CheckoutControllerStateHolder,
        shippingAddressElementStateHolder: ShippingAddressElementStateHolder,
        errorReporter: ErrorReporter,
    ) : this(
        activityResultCaller = activityResultCaller,
        lifecycleOwner = lifecycleOwner,
        paymentConfiguration = paymentConfiguration,
        coroutineScope = coroutineScope,
        commitShippingAddress = CommitShippingAddress(checkoutController::commitShippingAddress),
        stateHolder = stateHolder,
        shippingAddressElementStateHolder = shippingAddressElementStateHolder,
        errorReporter = errorReporter,
    )

    private val activityLauncher:
        ActivityResultLauncher<AddressElementActivityContract.Args.CheckoutShipping> =
        activityResultCaller.registerForActivityResult(
            AddressElementActivityContract.CheckoutShipping
        ) { result ->
            when (result) {
                is AddressElementActivityContract.Result.CheckoutShippingSucceeded -> {
                    val address = result.address.address?.toCheckoutAddress()
                    if (address == null) {
                        shippingAddressElementStateHolder.isPresenting = false
                    } else {
                        coroutineScope.launch {
                            try {
                                commitShippingAddress(
                                    result.updatedResponse,
                                    result.address.name,
                                    address,
                                )
                            } finally {
                                shippingAddressElementStateHolder.isPresenting = false
                            }
                        }
                    }
                }
                AddressElementActivityContract.Result.Canceled -> {
                    shippingAddressElementStateHolder.isPresenting = false
                }
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
            AddressElementActivityContract.Args.CheckoutShipping(
                publishableKey = paymentConfiguration.get().publishableKey,
                config = AddressLauncher.Configuration(
                    additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                        phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                    ),
                    billingAddress = null,
                    useStripeHostedAutocomplete = true,
                ),
                checkoutSessionResponse = requireNotNull(stateHolder.state).checkoutSessionResponse,
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
