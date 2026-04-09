package com.stripe.android.paymentsheet.model

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionCardArtDrawableLoader
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class PaymentOptionFactoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create() with GooglePay should return expected object`() {
        val factory = createFactory()
        val paymentOption = factory.create(PaymentSelection.GooglePay)
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption.label).isEqualTo("Google Pay")
        assertThat(paymentOption.paymentMethodType).isEqualTo("google_pay")
        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with card PaymentMethod should return expected object`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                )
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with card params should return expected object`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.New.Card(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                ),
                brand = CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with card and Link inline signup should return card icon and label`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.New.Card(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                ),
                brand = CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                linkInput = UserInput.SignUp(
                    email = "new_user@link.com",
                    phone = "+15555555555",
                    country = "US",
                    name = null,
                    consentAction = SignUpConsentAction.Checkbox,
                )
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with saved card should include billing details when present`() {
        val factory = createFactory()
        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setBillingDetails(PAYMENT_METHOD_BILLING_DETAILS)
            .setCard(PaymentMethod.Card(last4 = "4242", brand = CardBrand.Visa, displayBrand = "visa"))
            .build()

        val paymentOption = factory.create(PaymentSelection.Saved(paymentMethod))

        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with saved card should not include billing details when null`() {
        val factory = createFactory()
        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setCard(PaymentMethod.Card(last4 = "4242", brand = CardBrand.Visa, displayBrand = "visa"))
            .build()

        val paymentOption = factory.create(PaymentSelection.Saved(paymentMethod))

        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with new generic payment method should include billing details when present`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.New.GenericPaymentMethod(
                iconResource = R.drawable.stripe_ic_paymentsheet_card_unknown_ref,
                iconResourceNight = null,
                label = "Test Payment Method".resolvableString,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.PAYPAL.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                ),
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null
            )
        )

        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with Google Pay should not include billing details`() {
        val factory = createFactory()
        val paymentOption = factory.create(PaymentSelection.GooglePay)

        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with Link should not include billing details`() {
        val factory = createFactory()
        val paymentOption = factory.create(PaymentSelection.Link())

        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with CPM should include billing details when present`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.CustomPaymentMethod(
                id = "cpm_123",
                billingDetails = PAYMENT_METHOD_BILLING_DETAILS,
                label = "CPM".resolvableString,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            )
        )

        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with EPM should include billing details when present`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.ExternalPaymentMethod(
                type = "external_paypal",
                billingDetails = PAYMENT_METHOD_BILLING_DETAILS,
                label = "Paypal".resolvableString,
                iconResource = 0,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            )
        )

        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with partial billing details should map correctly`() {
        val factory = createFactory()
        val partialBillingDetails = PaymentMethod.BillingDetails(
            email = "test@example.com",
            name = "John Doe"
        )

        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setBillingDetails(partialBillingDetails)
            .setCard(PaymentMethod.Card(last4 = "4242", brand = CardBrand.Visa, displayBrand = "visa"))
            .build()

        val paymentOption = factory.create(PaymentSelection.Saved(paymentMethod))

        assertThat(paymentOption.billingDetails).isEqualTo(
            PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    city = null,
                    country = null,
                    line1 = null,
                    line2 = null,
                    postalCode = null,
                    state = null
                ),
                email = "test@example.com",
                name = "John Doe",
                phone = null
            )
        )
    }

    @Test
    fun `imageLoader uses card art drawable when loader returns one`() = runTest {
        val testBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val cardArtDrawable = testBitmap.toDrawable(
            ApplicationProvider.getApplicationContext<Context>().resources
        )
        val factory = createFactory(
            cardArtDrawableLoader = { cardArtDrawable },
        )

        factory.create(savedCardWithCardArt()).icon()
        // DelegateDrawable triggers imageLoader on init via Dispatchers.Main.immediate
    }

    @Test
    fun `imageLoader falls back to icon when card art loader returns null`() = runTest {
        val factory = createFactory(
            cardArtDrawableLoader = { null },
        )

        factory.create(savedCardWithCardArt()).icon()
    }

    private fun savedCardWithCardArt(): PaymentSelection.Saved {
        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setCard(
                PaymentMethod.Card(
                    last4 = "4242",
                    brand = CardBrand.Visa,
                    displayBrand = "visa",
                    cardArt = PaymentMethod.Card.CardArt(
                        artImage = PaymentMethod.Card.CardArt.ArtImage(
                            format = "image/png",
                            url = "https://example.com/card_art.png",
                        ),
                        programName = null,
                    ),
                )
            )
            .build()
        return PaymentSelection.Saved(paymentMethod)
    }

    private fun createFactory(
        cardArtDrawableLoader: PaymentOptionCardArtDrawableLoader = PaymentOptionCardArtDrawableLoader { null },
    ): PaymentOptionFactory {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return PaymentOptionFactory(
            iconLoader = PaymentSelection.IconLoader(
                resources = context.resources,
                imageLoader = DefaultStripeImageLoader(context),
            ),
            cardArtDrawableLoader = cardArtDrawableLoader,
            context = context,
        )
    }

    private companion object {
        const val CARD_ART_URL = "https://example.com/optimized.png"

        val PAYMENT_METHOD_BILLING_DETAILS = PaymentMethod.BillingDetails(
            address = Address(
                city = "San Francisco",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4B",
                postalCode = "94102",
                state = "CA"
            ),
            email = "test@example.com",
            name = "John Doe",
            phone = "+15555555555"
        )

        val PAYMENT_SHEET_BILLING_DETAILS = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4B",
                postalCode = "94102",
                state = "CA"
            ),
            email = "test@example.com",
            name = "John Doe",
            phone = "+15555555555"
        )
    }
}
