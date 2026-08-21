package com.stripe.android.checkout

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal sealed class CheckoutShippingAddressEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, Any>

    class Shown(
        checkoutSessionId: String?,
        address: AddressDetails?,
    ) : CheckoutShippingAddressEvent() {
        override val eventName: String = "checkout.shipping_address_element.shown"
        override val additionalParams: Map<String, Any> = params(
            checkoutSessionId = checkoutSessionId,
            address = address,
        )
    }

    class Canceled(
        checkoutSessionId: String?,
        address: AddressDetails?,
    ) : CheckoutShippingAddressEvent() {
        override val eventName: String = "checkout.shipping_address_element.canceled"
        override val additionalParams: Map<String, Any> = params(
            checkoutSessionId = checkoutSessionId,
            address = address,
        )
    }

    class SaveStarted(
        checkoutSessionId: String?,
        address: AddressDetails,
    ) : CheckoutShippingAddressEvent() {
        override val eventName: String = "checkout.shipping_address_element.save_started"
        override val additionalParams: Map<String, Any> = params(
            checkoutSessionId = checkoutSessionId,
            address = address,
        )
    }

    class SaveFailed(
        checkoutSessionId: String?,
        address: AddressDetails,
    ) : CheckoutShippingAddressEvent() {
        override val eventName: String = "checkout.shipping_address_element.save_failed"
        override val additionalParams: Map<String, Any> = params(
            checkoutSessionId = checkoutSessionId,
            address = address,
        )
    }

    class SaveCompleted(
        checkoutSessionId: String?,
        address: AddressDetails,
    ) : CheckoutShippingAddressEvent() {
        override val eventName: String = "checkout.shipping_address_element.save_completed"
        override val additionalParams: Map<String, Any> = params(
            checkoutSessionId = checkoutSessionId,
            address = address,
        )
    }

    private companion object {
        const val FIELD_CHECKOUT_SESSION_ID = "checkout_session_id"
        const val FIELD_ADDRESS_DATA_BLOB = "address_data_blob"
        const val FIELD_ADDRESS_COUNTRY_CODE = "address_country_code"

        fun params(
            checkoutSessionId: String?,
            address: AddressDetails?,
        ): Map<String, Any> = buildMap {
            checkoutSessionId?.let { put(FIELD_CHECKOUT_SESSION_ID, it) }
            address?.address?.country?.let { country ->
                put(
                    FIELD_ADDRESS_DATA_BLOB,
                    mapOf(FIELD_ADDRESS_COUNTRY_CODE to country),
                )
            }
        }
    }
}

internal interface CheckoutShippingAddressEventReporter {
    fun onShown(address: AddressDetails?)

    fun onCanceled(address: AddressDetails?)

    fun onSaveStarted(address: AddressDetails)

    fun onSaveFailed(address: AddressDetails)

    fun onSaveCompleted(address: AddressDetails)
}

internal class DefaultCheckoutShippingAddressEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val stateHolder: CheckoutControllerStateHolder,
    @IOContext private val workContext: CoroutineContext,
) : CheckoutShippingAddressEventReporter {
    override fun onShown(address: AddressDetails?) {
        fireEvent(
            CheckoutShippingAddressEvent.Shown(
                checkoutSessionId = stateHolder.state?.checkoutSessionResponse?.id,
                address = address,
            )
        )
    }

    override fun onCanceled(address: AddressDetails?) {
        fireEvent(
            CheckoutShippingAddressEvent.Canceled(
                checkoutSessionId = stateHolder.state?.checkoutSessionResponse?.id,
                address = address,
            )
        )
    }

    override fun onSaveStarted(address: AddressDetails) {
        fireEvent(
            CheckoutShippingAddressEvent.SaveStarted(
                checkoutSessionId = stateHolder.state?.checkoutSessionResponse?.id,
                address = address,
            )
        )
    }

    override fun onSaveFailed(address: AddressDetails) {
        fireEvent(
            CheckoutShippingAddressEvent.SaveFailed(
                checkoutSessionId = stateHolder.state?.checkoutSessionResponse?.id,
                address = address,
            )
        )
    }

    override fun onSaveCompleted(address: AddressDetails) {
        fireEvent(
            CheckoutShippingAddressEvent.SaveCompleted(
                checkoutSessionId = stateHolder.state?.checkoutSessionResponse?.id,
                address = address,
            )
        )
    }

    private fun fireEvent(event: CheckoutShippingAddressEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.additionalParams,
                )
            )
        }
    }
}
