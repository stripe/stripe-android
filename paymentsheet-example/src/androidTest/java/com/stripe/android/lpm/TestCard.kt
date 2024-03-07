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
    @Test
    fun testCard() {
        testDriver.confirmNewOrGuestComplete(
            TestParameters.create(
                paymentMethodCode = "card",
            ).copy(
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardWithCustomBillingDetailsCollection() {
        testDriver.confirmNewOrGuestComplete(
            TestParameters.create(
                paymentMethodCode = "card",
            ) { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.Off
                settings[CollectNameSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectEmailSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectPhoneSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            }.copy(
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
            ),
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
            TestParameters.create(
                paymentMethodCode = "card",
            ) { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
                settings[CollectNameSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectEmailSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectPhoneSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            }.copy(
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardInCustomFlow() {
        testDriver.confirmCustom(
            TestParameters.create(
                paymentMethodCode = "card",
            ).copy(
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
            ),
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
        val testParameters = TestParameters.create(
            paymentMethodCode = "card",
        ).copy(
            authorizationAction = null,
            saveForFutureUseCheckboxVisible = true,
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
            customerId = state?.customerConfig?.id,
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

        val testParameters = TestParameters.create(
            paymentMethodCode = "card",
        ).copy(
            authorizationAction = null,
            saveForFutureUseCheckboxVisible = true,
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
            customerId = state?.customerConfig?.id,
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = secondCardNumber,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCompleteWithDefaultSavedPaymentMethod(
            customerId = state?.customerConfig?.id,
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

        val testParameters = TestParameters.create(
            paymentMethodCode = "card",
        ).copy(
            authorizationAction = null,
            saveForFutureUseCheckboxVisible = true,
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
            customerId = state?.customerConfig?.id,
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

        val testParameters = TestParameters.create(
            paymentMethodCode = "card",
        ).copy(
            authorizationAction = null,
            saveForFutureUseCheckboxVisible = true,
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

        val customerId = state?.customerConfig?.id

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
            TestParameters.create(
                paymentMethodCode = "card",
            ).copy(
                authorizationAction = AuthorizeAction.Authorize3ds2,
                saveForFutureUseCheckboxVisible = true,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000000000003220")
        )
    }
}
