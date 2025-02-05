package com.stripe.android.lpm

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.settings.CollectAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectEmailSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectNameSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectPhoneSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.samples.ui.shared.PAYMENT_METHOD_SELECTOR_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCard : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "card",
        authorizationAction = null,
        executeInNightlyRun = true,
    ).copy(
        saveForFutureUseCheckboxVisible = true,
    )

    @Test
    fun testCard() {
        testDriver.confirmNewOrGuestComplete(
            testParameters,
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun test3DS2HSBCHTML() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.HSBCHTML,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000292"),
        )
    }

    @Test
    fun test3DS2OTP() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.OTP,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000045"),
        )
    }

    @Test
    fun test3DS2OOB() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.OOB,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000094"),
        )
    }

    @Test
    fun test3DS2SingleSelect() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.SingleSelect,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000102"),
        )
    }

    @Test
    fun test3DS2MultiSelect() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.MultiSelect,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000110"),
        )
    }

    @Test
    fun testCardWithCustomBillingDetailsCollection() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copyPlaygroundSettings { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.Off
                settings[CollectNameSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectEmailSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectPhoneSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            },
            populateCustomLpmFields = {
                populateCardDetails()
                populateEmail()
                populateName("Name on card")
                populateAddress()
                populatePhoneNumber()
            },
        )
    }

    @Test
    fun testCardWithCustomBillingDetailsCollectionWithDefaults() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copyPlaygroundSettings { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
                settings[CollectNameSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectEmailSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectPhoneSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardInCustomFlow() {
        testDriver.confirmCustom(
            testParameters,
            populateCustomLpmFields = {
                populateCardDetails()
            },
            verifyCustomLpmFields = {
                verifyCard()
            }
        )
    }

    /*
     * TODO(samer-stripe): Once we update `PaymentResult` to return a `StripeIntent`, update the test
     *  to check against the payment method IDs rather than the last 4 digits.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDefaultSavedPaymentMethodUsedAfterSingleSave() {
        val cardNumber = "6011111111111117"
        val testParameters = testParameters.copy(
            saveCheckboxValue = true,
        )

        val state = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = cardNumber,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCompleteWithDefaultSavedPaymentMethod(
            customerId = state?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters,
            beforeBuyAction = { selectors ->
                selectors.composeTestRule.waitUntilExactlyOneExists(
                    matcher = hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
                        .and(isSelected())
                        .and(hasText(cardNumber.takeLast(4), substring = true)),
                    timeoutMillis = 5000L
                )
            },
        )
    }

    /*
     * TODO(samer-stripe): Once we update `PaymentResult` to return a `StripeIntent`, update the test
     *  to check against the payment method IDs rather than the last 4 digits.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDefaultSavedPaymentMethodUsedAfterMultipleSaves() {
        val firstCardNumber = "6011111111111117"
        val secondCardNumber = "6011000990139424"

        val testParameters = testParameters.copy(
            saveCheckboxValue = true,
        )

        val state = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = firstCardNumber,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmExistingComplete(
            customerId = state?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = secondCardNumber,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCompleteWithDefaultSavedPaymentMethod(
            customerId = state?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters,
            beforeBuyAction = { selectors ->
                selectors.composeTestRule.waitUntilExactlyOneExists(
                    matcher = hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
                        .and(isSelected())
                        .and(hasText(secondCardNumber.takeLast(4), substring = true)),
                    timeoutMillis = 5000L
                )
            },
        )
    }

    /*
     * TODO(samer-stripe): Once we update `PaymentResult` to return a `StripeIntent`, update the test
     *  to check against the payment method IDs rather than the last 4 digits.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDefaultSavedPaymentMethodUsedAfterSingleSaveInCustomFlow() {
        val cardNumber = "6011111111111117"

        val testParameters = testParameters.copy(
            saveCheckboxValue = true,
        )

        val state = testDriver.confirmCustomAndBuy(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = cardNumber,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCustomWithDefaultSavedPaymentMethod(
            customerId = state?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters,
            beforeBuyAction = { selectors ->
                selectors.composeTestRule.waitUntilExactlyOneExists(
                    matcher = hasTestTag(PAYMENT_METHOD_SELECTOR_TEST_TAG)
                        .and(hasText(cardNumber.takeLast(4), substring = true)),
                    timeoutMillis = 5000L
                )
            },
        )
    }

    /*
     * TODO(samer-stripe): Once we update `PaymentResult` to return a `StripeIntent`, update the test
     *  to check against the payment method IDs rather than the last 4 digits.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDefaultSavedPaymentMethodUsedAfterMultipleSavesInCustomFlow() {
        val cardNumber = "6011111111111117"
        val secondCardNumber = "6011000990139424"

        val testParameters = testParameters.copy(
            saveCheckboxValue = true,
        )

        val state = testDriver.confirmCustomAndBuy(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = cardNumber,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        val customerId = state?.asPaymentState()?.customerConfig?.id

        testDriver.confirmCustomAndBuy(
            customerId = customerId,
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = secondCardNumber,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCustomWithDefaultSavedPaymentMethod(
            customerId = customerId,
            testParameters = testParameters,
            beforeBuyAction = { selectors ->
                selectors.composeTestRule.waitUntilExactlyOneExists(
                    matcher = hasTestTag(PAYMENT_METHOD_SELECTOR_TEST_TAG)
                        .and(hasText(secondCardNumber.takeLast(4), substring = true)),
                    timeoutMillis = 5000L
                )
            },
        )
    }

    @Test
    fun testCardWith3ds2() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Authorize3ds2,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000000000003220")
        )
    }

    @Test
    fun testCardWithCvcRecollectionComplete() {
        val testParameters = testParameters.copy(saveCheckboxValue = true)

        val state = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = "6011111111111117",
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCompleteWithSavePaymentMethodAndCvcRecollection(
            customerId = state?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters
        )
    }

    @Test
    fun testCardWithCvcRecollectionCustom() {
        val testParameters = testParameters.copy(saveCheckboxValue = true)

        val state = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = "6011111111111117",
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCustomWithSavePaymentMethodAndCvcRecollection(
            customerId = state?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters
        )
    }
}
