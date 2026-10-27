package com.stripe.android.checkout

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData
import com.stripe.android.core.reactnative.ReactNativeSdkInternal
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class, ReactNativeSdkInternal::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultCheckoutPaymentOptionFactoryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `create returns null when there is no selection`() = runScenario {
        assertThat(createOption(selection = null, paymentMethodMetadata = metadata)).isNull()
    }

    @Test
    fun `create returns null when there is no payment method metadata`() = runScenario {
        val option = createOption(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            paymentMethodMetadata = null,
        )

        assertThat(option).isNull()
    }

    @Test
    fun `create maps a Google Pay selection`() = runScenario {
        val option = createOption(selection = PaymentSelection.GooglePay, paymentMethodMetadata = metadata)

        assertThat(option).isNotNull()
        assertThat(option?.paymentMethodType).isEqualTo("google_pay")
        assertThat(option?.label).isEqualTo("Google Pay")
        assertThat(option?.sublabel).isNull()
        assertThat(option?.mandateText).isNull()
        assertThat(hasAccessedSepaMandate).isFalse()
    }

    @Test
    fun `create maps a new card selection`() = runScenario {
        val option = createOption(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            paymentMethodMetadata = metadata,
        )

        assertThat(option).isNotNull()
        assertThat(option?.paymentMethodType).isEqualTo("card")
        assertThat(option?.label).isEqualTo("Visa")
        assertThat(option?.sublabel).isEqualTo("\u2066···· 4242\u2069")
    }

    @Test
    fun `create attaches mandate text for a new card that requires setup`() = runScenario(
        // A SetupIntent forces the card form to require a mandate, unlike the default PaymentIntent metadata.
        metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_SUCCEEDED.copy(paymentMethodTypes = listOf("card")),
        ),
    ) {
        val option = createOption(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.mandateText).isNotNull()
        assertThat(hasAccessedSepaMandate).isFalse()
    }

    @Test
    fun `create maps a saved card without mandate text`() = runScenario {
        val option = createOption(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )

        assertThat(option).isNotNull()
        assertThat(option?.paymentMethodType).isEqualTo("card")
        assertThat(option?.label).isEqualTo("Visa")
        assertThat(option?.sublabel).isEqualTo("\u2066···· 4242\u2069")
        assertThat(option?.mandateText).isNull()
        assertThat(hasAccessedSepaMandate).isFalse()
    }

    @Test
    fun `reading a saved SEPA option mandate text records access`() = runScenario {
        val option = createOption(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )
        assertThat(option?.paymentMethodType).isEqualTo("sepa_debit")
        assertThat(hasAccessedSepaMandate).isFalse()

        assertThat(option?.mandateText?.text).contains(metadata.merchantName)

        assertThat(hasAccessedSepaMandate).isTrue()
    }

    @Test
    fun `create populates billing details from a saved payment method`() = runScenario {
        val option = createOption(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )

        val billingDetails = requireNotNull(option?.billingDetails)
        assertThat(billingDetails.name).isEqualTo("Jenny Rosen")
        assertThat(billingDetails.email).isEqualTo("jenny.rosen@example.com")
        assertThat(billingDetails.phone).isEqualTo("123-456-7890")
        assertThat(billingDetails.address?.line1).isEqualTo("1234 Main Street")
        assertThat(billingDetails.address?.city).isEqualTo("San Francisco")
        assertThat(billingDetails.address?.state).isEqualTo("CA")
        assertThat(billingDetails.address?.postalCode).isEqualTo("94111")
        assertThat(billingDetails.address?.country).isEqualTo("US")
    }

    @Test
    fun `create leaves billing details null for Google Pay`() = runScenario {
        val option = createOption(selection = PaymentSelection.GooglePay, paymentMethodMetadata = metadata)

        assertThat(option?.billingDetails).isNull()
    }

    @Test
    fun `create leaves address null when the payment method has billing details but no address`() = runScenario {
        val option = createOption(
            selection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "Jenny Rosen",
                        email = "jenny.rosen@example.com",
                        address = null,
                    ),
                ),
            ),
            paymentMethodMetadata = metadata,
        )

        val billingDetails = requireNotNull(option?.billingDetails)
        assertThat(billingDetails.name).isEqualTo("Jenny Rosen")
        assertThat(billingDetails.email).isEqualTo("jenny.rosen@example.com")
        assertThat(billingDetails.address).isNull()
    }

    @Test
    fun `iconPainter uses card art when the card art loader provides one`() = runScenario(
        cardArt = ColorDrawable(),
    ) {
        // Card art is only ever loaded for saved payment methods.
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val option = createOption(
            selection = selection,
            paymentMethodMetadata = metadata,
        )

        renderIcon(requireNotNull(option), isSystemDarkTheme = false)

        assertThat(cardArtLoadCalls.awaitItem()).isEqualTo(selection)
    }

    @Test
    fun `loadIcon uses light icon on light system`() = runScenario {
        val option = createOption(
            selection = customPaymentMethod,
            paymentMethodMetadata = metadata,
        )

        requireNotNull(option).loadIcon(isSystemDarkTheme = false)

        assertThat(imageLoader.awaitLoadCall().url).isEqualTo(LIGHT_ICON_URL)
    }

    @Test
    fun `loadIcon uses dark icon on dark system`() = runScenario {
        val option = createOption(
            selection = customPaymentMethod,
            paymentMethodMetadata = metadata,
        )

        requireNotNull(option).loadIcon(isSystemDarkTheme = true)

        assertThat(imageLoader.awaitLoadCall().url).isEqualTo(DARK_ICON_URL)
    }

    @Test
    fun `create uses card labels for saved Link passthrough card`() = runScenario(
        linkAccount = LinkAccount(
            TestFactory.CONSUMER_SESSION.copy(linkBrand = LinkBrand.Onelink),
        )
    ) {
        val option = createOption(
            selection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(isLinkPassthroughMode = true),
            ),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkBrand = LinkBrand.Link,
            ),
        )

        assertThat(option?.label).isEqualTo("Visa")
        assertThat(option?.sublabel).isEqualTo("\u2066···· 4242\u2069")
    }

    @Test
    fun `create maps a new bank account`() = runScenario {
        val option = createOption(
            selection = PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION,
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.label).isEqualTo("Stripe Bank")
        assertThat(option?.sublabel).isEqualTo("···· 6789")
    }

    @Test
    fun `create maps a saved bank account`() = runScenario {
        val option = createOption(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT),
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.label).isEqualTo("STRIPE TEST BANK")
        assertThat(option?.sublabel).isEqualTo("\u2066···· 6789\u2069")
    }

    @Test
    fun `create maps saved Link details using metadata brand`() = runScenario(
        metadata = PaymentMethodMetadataFactory.create(linkBrand = LinkBrand.Link),
    ) {
        val option = createOption(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.LINK_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.label).isEqualTo("Link")
        assertThat(option?.sublabel).isEqualTo("Visa Credit \u2066•••• 4242\u2069")
    }

    @Test
    fun `create maps saved Link details using account brand override`() = runScenario(
        metadata = PaymentMethodMetadataFactory.create(linkBrand = LinkBrand.Link),
        linkAccount = LinkAccount(TestFactory.CONSUMER_SESSION.copy(linkBrand = LinkBrand.Onelink)),
    ) {
        val option = createOption(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.LINK_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.label).isEqualTo("Onelink")
        assertThat(option?.sublabel).isEqualTo("Visa Credit \u2066•••• 4242\u2069")
    }

    @Test
    fun `reading a null card mandate does not acknowledge a later saved SEPA selection`() = runScenario {
        val stateHolder = createStateHolder(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        val cardOption = requireNotNull(stateHolder.session.value?.paymentOption)
        assertThat(cardOption.mandateText).isNull()
        stateHolder.setSelection(PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD))

        assertThat(requireNotNull(stateHolder.state).mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA))
            .isFalse()
    }

    @Test
    fun `reading one saved SEPA mandate does not acknowledge another saved SEPA selection`() = runScenario {
        val stateHolder = createStateHolder(SAVED_SEPA)
        assertThat(stateHolder.session.value?.paymentOption?.mandateText).isNotNull()
        val anotherPaymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD.copy(id = "pm_another_sepa")
        stateHolder.setSelection(PaymentSelection.Saved(anotherPaymentMethod))

        assertThat(
            requireNotNull(stateHolder.state).mandateState.hasAcknowledgedSepaMandate(
                PaymentSelection.Saved(anotherPaymentMethod)
            )
        ).isFalse()
    }

    @Test
    fun `reading an old payment option does not acknowledge saved SEPA after reconfiguration`() = runScenario {
        val stateHolder = createStateHolder(SAVED_SEPA)
        val oldOption = requireNotNull(stateHolder.session.value?.paymentOption)
        stateHolder.state = requireNotNull(stateHolder.state).copy(mandateState = CheckoutMandateState.initial())
        assertThat(oldOption.mandateText).isNotNull()

        assertThat(requireNotNull(stateHolder.state).mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA))
            .isFalse()
    }

    @Test
    fun `reading the current payment option acknowledges saved SEPA after reconfiguration`() = runScenario {
        val stateHolder = createStateHolder(SAVED_SEPA)
        stateHolder.state = requireNotNull(stateHolder.state).copy(mandateState = CheckoutMandateState.initial())
        assertThat(stateHolder.session.value?.paymentOption?.mandateText).isNotNull()

        assertThat(requireNotNull(stateHolder.state).mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA))
            .isTrue()
    }

    @Test
    fun `reading an old payment option does not clear current mandate acknowledgement`() = runScenario {
        val stateHolder = createStateHolder(SAVED_SEPA)
        val oldOption = requireNotNull(stateHolder.session.value?.paymentOption)
        stateHolder.state = requireNotNull(stateHolder.state).copy(mandateState = CheckoutMandateState.initial())
        assertThat(stateHolder.session.value?.paymentOption?.mandateText).isNotNull()
        assertThat(oldOption.mandateText).isNotNull()

        assertThat(requireNotNull(stateHolder.state).mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA))
            .isTrue()
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        cardArt: Drawable? = null,
        linkAccount: LinkAccount? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = FakeStripeImageLoader()
        val cardArtLoadCalls = Turbine<PaymentSelection>()
        val mandateState = CheckoutMandateState.initial()
        Scenario(
            factory = DefaultCheckoutPaymentOptionDisplayDataFactory(
                iconLoader = PaymentSelection.IconLoader(
                    resources = context.resources,
                    imageLoader = imageLoader,
                ),
                cardArtDrawableLoader = { selection ->
                    if (cardArt != null) {
                        cardArtLoadCalls.add(selection)
                    }
                    cardArt
                },
                context = context,
                linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply {
                    set(LinkAccountUpdate.Value(linkAccount))
                },
            ),
            metadata = metadata,
            cardArtLoadCalls = cardArtLoadCalls,
            imageLoader = imageLoader,
            mandateState = mandateState,
        ).apply { block() }
        cardArtLoadCalls.ensureAllEventsConsumed()
        imageLoader.ensureAllEventsConsumed()
    }

    private fun renderIcon(
        option: PaymentOptionDisplayData,
        isSystemDarkTheme: Boolean,
    ) {
        composeRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                val nightMode = if (isSystemDarkTheme) {
                    Configuration.UI_MODE_NIGHT_YES
                } else {
                    Configuration.UI_MODE_NIGHT_NO
                }
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                option.iconPainter
            }
        }
        composeRule.waitForIdle()
    }

    private class Scenario(
        val factory: DefaultCheckoutPaymentOptionDisplayDataFactory,
        val metadata: PaymentMethodMetadata,
        val cardArtLoadCalls: ReceiveTurbine<PaymentSelection>,
        val imageLoader: FakeStripeImageLoader,
        val mandateState: CheckoutMandateState,
    ) {
        val hasAccessedSepaMandate: Boolean
            get() = mandateState.hasAcknowledgedSepaMandate(
                PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
            )

        fun createStateHolder(selection: PaymentSelection): CheckoutControllerStateHolder {
            return CheckoutControllerStateFactory.createStateHolder(
                savedStateHandle = SavedStateHandle(),
                paymentOptionFactory = factory,
            ).apply {
                state = CheckoutControllerStateFactory.create(
                    paymentSelection = selection,
                    paymentMethodMetadata = metadata,
                    mandateState = mandateState,
                )
            }
        }

        fun createOption(
            selection: PaymentSelection?,
            paymentMethodMetadata: PaymentMethodMetadata? = metadata,
        ) = factory.create(selection, paymentMethodMetadata, mandateState)
    }

    private companion object {
        const val LIGHT_ICON_URL = "light_icon_url"
        const val DARK_ICON_URL = "dark_icon_url"

        val SAVED_SEPA = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)

        val customPaymentMethod = PaymentSelection.CustomPaymentMethod(
            id = "cpm_123",
            billingDetails = null,
            label = "Custom".resolvableString,
            lightThemeIconUrl = LIGHT_ICON_URL,
            darkThemeIconUrl = DARK_ICON_URL,
        )
    }
}
