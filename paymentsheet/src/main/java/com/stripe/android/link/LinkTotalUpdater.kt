package com.stripe.android.link

import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.InvocationSource
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.link.injection.NativeLinkScope
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.checkoutSessionResponse
import com.stripe.android.model.Address
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal typealias LinkTaxRegionUpdate = suspend (
    CheckoutSessionResponse,
    CheckoutElementsController.Address.State,
) -> Result<CheckoutSessionResponse>

/**
 * Keeps the total shown in native Link in sync when it depends on the billing address of the selected payment
 * details, e.g. for automatic tax calculated from a Checkout Session's billing address.
 *
 * Updates run one at a time in the order they were requested, so the server applies them in order.
 */
@OptIn(CheckoutSessionPreview::class)
@NativeLinkScope
internal class LinkTotalUpdater internal constructor(
    initialCheckoutSessionResponse: CheckoutSessionResponse?,
    private val updateTaxRegion: LinkTaxRegionUpdate,
) {
    @Inject
    constructor(
        paymentMethodMetadata: PaymentMethodMetadata,
        linkLaunchMode: LinkLaunchMode,
        taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
    ) : this(
        initialCheckoutSessionResponse = paymentMethodMetadata.checkoutSessionResponse?.takeIf {
            it.collectsTaxFromBillingAddress && linkLaunchMode.showsTotal
        },
        updateTaxRegion = { response, address ->
            val clientAttributionMetadata = paymentMethodMetadata.clientAttributionMetadata
            val isExpressCheckoutElement =
                (clientAttributionMetadata as? ClientAttributionMetadata.CheckoutSession)?.isExpressCheckoutElement == true
            taxRegionUpdater.updateServerStateIfNeeded(
                checkoutSessionResponse = response,
                addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                invocationSource = if (isExpressCheckoutElement) {
                    InvocationSource.ExpressCheckoutElement
                } else {
                    InvocationSource.PaymentElement
                },
                address = address,
            )
        },
    )

    data class State(
        /** The latest Checkout Session, or null if the total doesn't depend on the billing address. */
        val checkoutSessionResponse: CheckoutSessionResponse?,
        /** Whether an update is in flight that the UI should wait for. */
        val isUpdating: Boolean,
    ) {
        /** The total to display, or null to use the intent's amount. */
        val amount: Long?
            get() = checkoutSessionResponse?.amount
    }

    private val _state = MutableStateFlow(
        State(checkoutSessionResponse = initialCheckoutSessionResponse, isUpdating = false)
    )
    val state: StateFlow<State> = _state.asStateFlow()

    private val mutex = Mutex()
    private var latestRequestId = 0
    private var syncedAddress: CheckoutElementsController.Address.State? = null

    /**
     * Syncs the total for the given billing address.
     *
     * @param showsProgress Whether to set [State.isUpdating] while the request is in flight.
     * @return Whether the total changed from the one displayed when this method was called.
     */
    suspend fun sync(billingAddress: Address?, showsProgress: Boolean): Result<Boolean> {
        if (_state.value.checkoutSessionResponse == null) {
            return Result.success(false)
        }
        // Without a country there's nothing to calculate tax from.
        val address = billingAddress?.toCheckoutAddress() ?: return Result.success(false)
        if (!mutex.isLocked && address == syncedAddress) {
            return Result.success(false)
        }

        val amountBefore = _state.value.amount
        val requestId = ++latestRequestId
        if (showsProgress) {
            _state.update { it.copy(isUpdating = true) }
        }

        return try {
            mutex.withLock {
                if (address == syncedAddress) {
                    return@withLock Result.success(_state.value.amount != amountBefore)
                }
                val currentResponse = requireNotNull(_state.value.checkoutSessionResponse)
                updateTaxRegion(currentResponse, address)
                    .onSuccess { response ->
                        syncedAddress = address
                        _state.update { it.copy(checkoutSessionResponse = response) }
                    }
                    .onFailure {
                        syncedAddress = null
                    }
                    .map { response -> response.amount != amountBefore }
            }
        } finally {
            if (requestId == latestRequestId) {
                _state.update { it.copy(isUpdating = false) }
            }
        }
    }

    /**
     * Returns [paymentMethodMetadata] with the latest Checkout Session, so that confirmation expects the total the
     * customer saw.
     */
    fun applyTo(paymentMethodMetadata: PaymentMethodMetadata): PaymentMethodMetadata {
        val response = _state.value.checkoutSessionResponse ?: return paymentMethodMetadata
        val integrationMetadata = paymentMethodMetadata.integrationMetadata as? IntegrationMetadata.CheckoutSession
            ?: return paymentMethodMetadata
        return paymentMethodMetadata.copy(
            integrationMetadata = integrationMetadata.copy(checkoutSessionResponse = response),
        )
    }
}

/** The total is only shown, and therefore only updated, when Link confirms the payment itself. */
private val LinkLaunchMode.showsTotal: Boolean
    get() = when (this) {
        is LinkLaunchMode.Full,
        is LinkLaunchMode.Confirmation -> true
        is LinkLaunchMode.PaymentMethodSelection,
        is LinkLaunchMode.Authentication,
        is LinkLaunchMode.Authorization -> false
    }

internal fun ConsumerPaymentDetails.BillingAddress.toAddress(): Address {
    return Address(
        line1 = line1,
        line2 = line2,
        postalCode = postalCode,
        city = locality,
        state = administrativeArea,
        country = countryCode?.value,
    )
}
