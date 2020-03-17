package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.DateOfBirth
import com.stripe.android.model.KlarnaSourceParams
import com.stripe.android.model.SourceOrder
import com.stripe.android.model.SourceParams
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SourceEndToEndTest {
    @Test
    fun createKlarnaParams_createsExpectedSourceOrderItems() {
        val sourceParams = SourceParams.createKlarna(
            returnUrl = RETURN_URL,
            currency = "eur",
            klarnaParams = KlarnaSourceParams(
                purchaseCountry = "DE",
                lineItems = LINE_ITEMS
            )
        )
        val stripe = createStripe(ApiKeyFixtures.KLARNA_PUBLISHABLE_KEY)

        val source = requireNotNull(stripe.createSourceSynchronous(sourceParams))
        assertThat(source.redirect?.returnUrl)
            .isEqualTo(RETURN_URL)
        assertThat(source.amount)
            .isEqualTo(31999)

        val items = requireNotNull(source.sourceOrder?.items)
        assertThat(items)
            .hasSize(4)
        assertThat(items.first())
            .isEqualTo(SourceOrder.Item(
                type = SourceOrder.Item.Type.Sku,
                amount = 10000,
                currency = "eur",
                description = "towel",
                quantity = 1
            ))
    }

    @Test
    fun createFullKlarnaParams() {
        val sourceParams = SourceParams.createKlarna(
            returnUrl = RETURN_URL,
            currency = "GBP",
            klarnaParams = KlarnaSourceParams(
                purchaseCountry = "UK",
                lineItems = LINE_ITEMS,
                billingPhone = "02012267709",
                billingEmail = "test@example.com",
                billingAddress = Address(
                    line1 = "29 Arlington Avenue",
                    city = "London",
                    country = "UK",
                    postalCode = "N1 7BE"
                ),
                billingFirstName = "Arthur",
                billingLastName = "Dent",
                billingDob = DateOfBirth(11, 3, 1952)
            )
        )
        val stripe = createStripe(ApiKeyFixtures.KLARNA_PUBLISHABLE_KEY)

        val source = requireNotNull(stripe.createSourceSynchronous(sourceParams))
        assertThat(source.redirect?.returnUrl)
            .isEqualTo(RETURN_URL)
    }

    @Test
    fun createKlarnaParamsWithCustomPaymentMethods() {
        val sourceParams = SourceParams.createKlarna(
            returnUrl = RETURN_URL,
            currency = "USD",
            klarnaParams = KlarnaSourceParams(
                purchaseCountry = "US",
                lineItems = LINE_ITEMS,
                customPaymentMethods = setOf(
                    KlarnaSourceParams.CustomPaymentMethods.Installments,
                    KlarnaSourceParams.CustomPaymentMethods.PayIn4
                )
            )
        )

        val stripe = createStripe(ApiKeyFixtures.KLARNA_PUBLISHABLE_KEY)
        val source = requireNotNull(stripe.createSourceSynchronous(sourceParams))
        assertThat(source.redirect?.returnUrl)
            .isEqualTo(RETURN_URL)
    }

    @Test
    fun createKlarnaParamsWithPageOptions() {
        val sourceParams = SourceParams.createKlarna(
            returnUrl = RETURN_URL,
            currency = "eur",
            klarnaParams = KlarnaSourceParams(
                purchaseCountry = "DE",
                lineItems = LINE_ITEMS,
                pageOptions = KlarnaSourceParams.PaymentPageOptions(
                    pageTitle = "Very cool checkout page",
                    purchaseType = KlarnaSourceParams.PaymentPageOptions.PurchaseType.Order
                )
            )
        )
        val stripe = createStripe(ApiKeyFixtures.KLARNA_PUBLISHABLE_KEY)
        val source = requireNotNull(stripe.createSourceSynchronous(sourceParams))
        assertThat(source.redirect?.returnUrl)
            .isEqualTo(RETURN_URL)
    }

    private fun createStripe(publishableKey: String): Stripe {
        return Stripe(
            ApplicationProvider.getApplicationContext(),
            publishableKey
        )
    }

    private companion object {
        private const val RETURN_URL = "https://example.com"

        private val LINE_ITEMS = listOf(
            KlarnaSourceParams.LineItem(
                itemType = KlarnaSourceParams.LineItem.Type.Sku,
                itemDescription = "towel",
                totalAmount = 10000,
                quantity = 1
            ),
            KlarnaSourceParams.LineItem(
                itemType = KlarnaSourceParams.LineItem.Type.Sku,
                itemDescription = "digital watch",
                totalAmount = 20000,
                quantity = 2
            ),
            KlarnaSourceParams.LineItem(
                itemType = KlarnaSourceParams.LineItem.Type.Tax,
                itemDescription = "taxes",
                totalAmount = 1500
            ),
            KlarnaSourceParams.LineItem(
                itemType = KlarnaSourceParams.LineItem.Type.Shipping,
                itemDescription = "ground shipping",
                totalAmount = 499
            )
        )
    }
}
