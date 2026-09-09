package com.stripe.android.elements

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutOperationCoordinator
import com.stripe.android.checkout.ShippingAddressElementStateHolder
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider

@OptIn(CheckoutSessionPreview::class)
internal fun interface CommitShippingAddress {
    suspend operator fun invoke(
        name: String?,
        address: CheckoutController.Address.State,
    ): Result<Unit>
}

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShippingAddressElement internal constructor(
    activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val commitShippingAddress: CommitShippingAddress,
    private val stateHolder: CheckoutControllerStateHolder,
    private val shippingAddressElementStateHolder: ShippingAddressElementStateHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val isUpdating: StateFlow<Boolean>,
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
        sheetStateHolder: SheetStateHolder,
        operationCoordinator: CheckoutOperationCoordinator,
        errorReporter: ErrorReporter,
    ) : this(
        activityResultCaller = activityResultCaller,
        lifecycleOwner = lifecycleOwner,
        paymentConfiguration = paymentConfiguration,
        coroutineScope = coroutineScope,
        commitShippingAddress = CommitShippingAddress(checkoutController::commitShippingAddress),
        stateHolder = stateHolder,
        shippingAddressElementStateHolder = shippingAddressElementStateHolder,
        sheetStateHolder = sheetStateHolder,
        isUpdating = operationCoordinator.isUpdating,
        errorReporter = errorReporter,
    )

    private val activityLauncher:
        ActivityResultLauncher<AddressElementActivityContract.Args.CheckoutShipping> =
        activityResultCaller.registerForActivityResult(
            AddressElementActivityContract.CheckoutShipping
        ) { result ->
            clearPresentation()
            when (result) {
                is AddressElementActivityContract.Result.CheckoutShippingSucceeded -> {
                    val address = result.address.address?.toCheckoutAddress()
                    if (address != null) {
                        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                            commitShippingAddress(
                                result.address.name,
                                address,
                            )
                        }
                    }
                }
                AddressElementActivityContract.Result.Canceled -> Unit
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

        resumePendingReadyLaunch()
    }

    fun present() {
        if (stateHolder.state == null) {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.CHECKOUT_SHIPPING_ADDRESS_ELEMENT_PRESENT_NOT_CONFIGURED
            )
            return
        }

        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        try {
            val publishableKey = paymentConfiguration.get().publishableKey
            if (isUpdating.value) {
                shippingAddressElementStateHolder.isAwaitingReady = true
                activityLauncher.launch(
                    AddressElementActivityContract.Args.CheckoutShipping.Loading(publishableKey)
                )
                resumePendingReadyLaunch()
            } else {
                launchReady(publishableKey)
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            clearPresentation()
            throw error
        }
    }

    private fun resumePendingReadyLaunch() {
        if (!shippingAddressElementStateHolder.isAwaitingReady) {
            return
        }
        lifecycleOwner.lifecycleScope.launch {
            isUpdating.first { isUpdating -> !isUpdating }
            if (!shippingAddressElementStateHolder.isAwaitingReady) {
                return@launch
            }
            if (stateHolder.state == null) {
                errorReporter.report(
                    ErrorReporter.ExpectedErrorEvent.CHECKOUT_SHIPPING_ADDRESS_ELEMENT_PRESENT_NOT_CONFIGURED
                )
                return@launch
            }
            try {
                val publishableKey = paymentConfiguration.get().publishableKey
                launchReady(publishableKey)
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                clearPresentation()
                throw error
            }
        }
    }

    private fun launchReady(publishableKey: String) {
        shippingAddressElementStateHolder.isAwaitingReady = false
        activityLauncher.launch(
            AddressElementActivityContract.Args.CheckoutShipping.Ready(
                publishableKey = publishableKey,
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

    private fun clearPresentation() {
        shippingAddressElementStateHolder.isAwaitingReady = false
        sheetStateHolder.sheetIsOpen = false
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
