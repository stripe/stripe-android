@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.validateShippingCountry
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

internal class CheckoutShippingAddressProcessor internal constructor(
    private val updateTaxRegion: UpdateCheckoutShippingAddress,
) {
    @Inject
    constructor(checkoutSessionTaxRegionUpdater: CheckoutSessionTaxRegionUpdater) : this(
        updateTaxRegion = checkoutSessionTaxRegionUpdater::updateServerStateIfNeeded,
    )

    @Suppress("TooGenericExceptionCaught")
    suspend fun process(
        checkoutSessionResponse: CheckoutSessionResponse,
        addressDetails: AddressDetails,
    ): Result<CheckoutSessionResponse> {
        return try {
            val address = addressDetails.address?.toCheckoutAddress()
                ?: error("Shipping address country is required.")
            checkoutSessionResponse.validateShippingCountry(address.country).getOrThrow()
            updateTaxRegion(
                checkoutSessionResponse,
                CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                address,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

internal typealias UpdateCheckoutShippingAddress = suspend (
    checkoutSessionResponse: CheckoutSessionResponse,
    addressSource: CheckoutSessionResponse.TaxAddressSource,
    address: com.stripe.android.checkout.CheckoutController.Address.State,
) -> Result<CheckoutSessionResponse>
