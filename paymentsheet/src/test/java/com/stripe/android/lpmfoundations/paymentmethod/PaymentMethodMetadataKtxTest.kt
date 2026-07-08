package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import org.junit.Test

internal class PaymentMethodMetadataKtxTest {

    @Test
    fun `toSaveConsentBehavior returns Enabled when checkout offer save is enabled`() {
        val offerSave = CheckoutSessionResponse.SavedPaymentMethodsOfferSave(
            enabled = true,
            status = CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED,
        )

        val result = offerSave.toSaveConsentBehavior()

        assertThat(result).isEqualTo(PaymentMethodSaveConsentBehavior.Enabled)
    }

    @Test
    fun `toSaveConsentBehavior returns Disabled when checkout offer save is disabled`() {
        val offerSave = CheckoutSessionResponse.SavedPaymentMethodsOfferSave(
            enabled = false,
            status = CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED,
        )

        val result = offerSave.toSaveConsentBehavior()

        assertThat(result).isEqualTo(
            PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null)
        )
    }

    @Test
    fun `toPaymentSheetSaveConsentBehavior returns Enabled from customer session`() {
        val elementsSession = createElementsSession(
            customer = createCustomerWithMPE(isPaymentMethodSaveEnabled = true)
        )

        val result = elementsSession.toPaymentSheetSaveConsentBehavior()

        assertThat(result).isEqualTo(PaymentMethodSaveConsentBehavior.Enabled)
    }

    @Test
    fun `toPaymentSheetSaveConsentBehavior returns Disabled from customer session`() {
        val allowRedisplayOverride = PaymentMethod.AllowRedisplay.ALWAYS
        val elementsSession = createElementsSession(
            customer = createCustomerWithMPE(
                isPaymentMethodSaveEnabled = false,
                allowRedisplayOverride = allowRedisplayOverride,
            )
        )

        val result = elementsSession.toPaymentSheetSaveConsentBehavior()

        assertThat(result).isEqualTo(
            PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = allowRedisplayOverride)
        )
    }

    @Test
    fun `toPaymentSheetSaveConsentBehavior returns Legacy when no customer`() {
        val elementsSession = createElementsSession(customer = null)

        val result = elementsSession.toPaymentSheetSaveConsentBehavior()

        assertThat(result).isEqualTo(PaymentMethodSaveConsentBehavior.Legacy)
    }

    @Test
    fun `effectiveLinkBrand returns account linkBrand when account is present`() {
        val metadata = PaymentMethodMetadataFactory.create(linkBrand = LinkBrand.Link)
        val account = LinkAccount(makeConsumerSession(linkBrand = LinkBrand.Onelink))

        assertThat(metadata.effectiveLinkBrand(account)).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `effectiveLinkBrand returns metadata linkBrand when account is null`() {
        val metadata = PaymentMethodMetadataFactory.create(linkBrand = LinkBrand.Onelink)

        assertThat(metadata.effectiveLinkBrand(null)).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `effectiveLinkBrand returns default Link when account is null and metadata uses default`() {
        val metadata = PaymentMethodMetadataFactory.create()

        assertThat(metadata.effectiveLinkBrand(null)).isEqualTo(LinkBrand.Link)
    }

    private fun makeConsumerSession(linkBrand: LinkBrand?): ConsumerSession {
        return ConsumerSession(
            clientSecret = "secret_123",
            emailAddress = "test@example.com",
            redactedPhoneNumber = "+1********07",
            redactedFormattedPhoneNumber = "(***) *** **07",
            verificationSessions = emptyList(),
            linkBrand = linkBrand,
        )
    }

    private fun createElementsSession(
        customer: ElementsSession.Customer? = null,
    ): ElementsSession {
        return ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = null,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = customer,
            linkSettings = null,
            orderedPaymentMethodTypesAndWallets = listOf("card"),
            customPaymentMethods = emptyList(),
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            elementsSessionId = "session_1234",
            flags = emptyMap(),
            experimentsData = null,
            passiveCaptcha = null,
            merchantLogoUrl = null,
            elementsSessionConfigId = null,
            accountId = "acct_1SGP1sPvdtoA7EjP",
            merchantId = "acct_1SGP1sPvdtoA7EjP",
        )
    }

    private fun createCustomerWithMPE(
        isPaymentMethodSaveEnabled: Boolean,
        allowRedisplayOverride: PaymentMethod.AllowRedisplay? = null,
    ): ElementsSession.Customer {
        return ElementsSession.Customer(
            paymentMethods = emptyList(),
            defaultPaymentMethod = null,
            email = null,
            session = ElementsSession.Customer.Session(
                id = "cuss_1",
                customerId = "cus_1",
                apiKey = "ek_1",
                apiKeyExpiry = 999999999,
                liveMode = false,
                components = ElementsSession.Customer.Components(
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                    mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                        isPaymentMethodSaveEnabled = isPaymentMethodSaveEnabled,
                        paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
                        paymentMethodRemoveLast =
                        ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                        allowRedisplayOverride = allowRedisplayOverride,
                        isPaymentMethodSetAsDefaultEnabled = false,
                    )
                )
            ),
        )
    }
}
