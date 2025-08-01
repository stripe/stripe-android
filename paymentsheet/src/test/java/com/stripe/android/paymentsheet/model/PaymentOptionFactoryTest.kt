package com.stripe.android.paymentsheet.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.BillingDetails
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.image.StripeImageLoader
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class PaymentOptionFactoryTest {

    private val factory = PaymentOptionFactory(
        iconLoader = PaymentSelection.IconLoader(
            resources = ApplicationProvider.getApplicationContext<Context>().resources,
            imageLoader = StripeImageLoader(ApplicationProvider.getApplicationContext()),
        ),
        context = ApplicationProvider.getApplicationContext(),
    )

    @Test
    fun `create() with GooglePay should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.GooglePay
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption.label).isEqualTo("Google Pay")
        assertThat(paymentOption.paymentMethodType).isEqualTo("google_pay")
        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with card PaymentMethod should return expected object`() {
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
    fun `create() with saved card params with known brand from wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(CardBrand.Visa),
                walletType = PaymentSelection.Saved.WalletType.GooglePay
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with saved card params with unknown brand from Link wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(),
                walletType = PaymentSelection.Saved.WalletType.Link
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_ref)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with saved card params without last 4 digits from Link wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(last4 = null),
                walletType = PaymentSelection.Saved.WalletType.Link
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_ref)
        assertThat(paymentOption.label).isEqualTo("Link")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with saved card params with unknown brand from Google wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(),
                walletType = PaymentSelection.Saved.WalletType.GooglePay
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with saved card params without last 4 digits from Google wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(last4 = null),
                walletType = PaymentSelection.Saved.WalletType.GooglePay
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption.label).isEqualTo("Google Pay")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with card and Link inline signup should return card icon and label`() {
        val paymentOption = factory.create(
            PaymentSelection.New.LinkInline(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                ),
                brand = CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                input = UserInput.SignUp(
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
        val paymentOption = factory.create(
            PaymentSelection.New.GenericPaymentMethod(
                iconResource = R.drawable.stripe_ic_paymentsheet_card_unknown_ref,
                label = "Test Payment Method".resolvableString,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.SOFORT.copy(
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
        val paymentOption = factory.create(PaymentSelection.GooglePay)

        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with Link should not include billing details`() {
        val paymentOption = factory.create(PaymentSelection.Link())

        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with CPM should include billing details when present`() {
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

        val paymentOption = factory.create(
            PaymentSelection.Saved(paymentMethod)
        )

        assertThat(paymentOption.billingDetails).isEqualTo(
            BillingDetails(
                address = com.stripe.android.elements.Address(
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

    private fun card(
        brand: CardBrand = CardBrand.Unknown,
        last4: String? = "4242"
    ): PaymentMethod {
        return PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setCard(PaymentMethod.Card(last4 = last4, brand = brand, displayBrand = brand.code))
            .build()
    }

    private companion object {
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

        val PAYMENT_SHEET_BILLING_DETAILS = BillingDetails(
            address = com.stripe.android.elements.Address(
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
