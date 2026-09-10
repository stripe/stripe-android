package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties.TextSelectionRange
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.LayoutDirection
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.NoOpCardScanEventsReporter
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.elements.events.LocalCardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RightToLeftCardFormTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `card number cursor is placed at the end`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(CARD_NUMBER, TextRange(CARD_NUMBER.length))
    }

    @Test
    fun `expiration date cursor is placed at the end`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(EXPIRATION_DATE, TextRange(EXPIRATION_DATE.length))
    }

    @Test
    fun `CVC cursor is placed at the end`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(CVC, TextRange(CVC.length))
    }

    @Test
    fun `US ZIP code cursor is placed at the end`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(US_ZIP_CODE, TextRange(US_ZIP_CODE.length))
    }

    @Test
    fun `Canadian postal code cursor is placed at the end`() = runScenario(CA_BILLING_DETAILS) {
        assertCursorAtRightEdge(CA_POSTAL_CODE, TextRange(CA_POSTAL_CODE.length))
    }

    @Test
    fun `UK postal code cursor is placed at the end`() = runScenario(GB_BILLING_DETAILS) {
        assertCursorAtRightEdge(GB_POSTAL_CODE, TextRange(GB_POSTAL_CODE.length))
    }

    @Test
    fun `name cursor is placed at the start`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(NAME, TextRange.Zero)
    }

    @Test
    fun `email cursor is placed at the end`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(EMAIL, TextRange(EMAIL.length))
    }

    @Test
    fun `address line 1 cursor is placed at the start`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(ADDRESS_LINE_1, TextRange.Zero)
    }

    @Test
    fun `address line 2 cursor is placed at the start`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(ADDRESS_LINE_2, TextRange.Zero)
    }

    @Test
    fun `city cursor is placed at the start`() = runScenario(US_BILLING_DETAILS) {
        assertCursorAtRightEdge(CITY, TextRange.Zero)
    }

    private fun runScenario(
        billingDetails: BillingDetails,
        block: () -> Unit,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalCardScanEventsReporter provides NoOpCardScanEventsReporter,
                LocalCardNumberCompletedEventReporter provides { },
                LocalCardBrandDisallowedReporter provides { },
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CardDefinition.CreateFormUi(
                        metadata = METADATA,
                        paymentMethodCreateParams = createPaymentMethodCreateParams(billingDetails),
                    )
                }
            }
        }

        block()
    }

    private fun assertCursorAtRightEdge(value: String, expectedSelection: TextRange) {
        composeRule
            .onNode(hasText(value))
            .performScrollTo()
            .performTouchInput {
                click(Offset(visibleSize.width - 1f, visibleSize.height / 2f))
            }
            .assert(SemanticsMatcher.expectValue(TextSelectionRange, expectedSelection))
    }

    private fun createPaymentMethodCreateParams(
        billingDetails: BillingDetails,
    ): PaymentMethodCreateParams {
        return PaymentMethodCreateParams.createWithOverride(
            code = "card",
            billingDetails = null,
            requiresMandate = false,
            overrideParamMap = mapOf(
                "type" to "card",
                "card" to mapOf(
                    "number" to CARD_NUMBER,
                    "exp_month" to "07",
                    "exp_year" to "2050",
                    "cvc" to CVC,
                ),
                "billing_details" to mapOf(
                    "name" to NAME,
                    "phone" to billingDetails.phone,
                    "email" to EMAIL,
                    "address" to mapOf(
                        "country" to billingDetails.country,
                        "line1" to ADDRESS_LINE_1,
                        "line2" to ADDRESS_LINE_2,
                        "state" to billingDetails.state,
                        "city" to CITY,
                        "postal_code" to billingDetails.postalCode,
                    ),
                ),
            ),
            productUsage = emptySet(),
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
        )
    }

    private data class BillingDetails(
        val country: String,
        val state: String,
        val postalCode: String,
        val phone: String,
    )

    private companion object {
        const val CARD_NUMBER = "4242424242424242"
        const val EXPIRATION_DATE = "0750"
        const val CVC = "123"
        const val NAME = "محمد الأحمد"
        const val EMAIL = "johndoe@email.com"
        const val ADDRESS_LINE_1 = "١٢٣ شارع التفاح"
        const val ADDRESS_LINE_2 = "الطابق الثاني"
        const val CITY = "الرياض"
        const val US_ZIP_CODE = "94080"
        const val CA_POSTAL_CODE = "M5V3A8"
        const val GB_POSTAL_CODE = "SW1A1AA"

        val METADATA = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card"),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )

        val US_BILLING_DETAILS = BillingDetails(
            country = "US",
            state = "CA",
            postalCode = US_ZIP_CODE,
            phone = "+14155552671",
        )

        val CA_BILLING_DETAILS = BillingDetails(
            country = "CA",
            state = "ON",
            postalCode = CA_POSTAL_CODE,
            phone = "+14165550123",
        )

        val GB_BILLING_DETAILS = BillingDetails(
            country = "GB",
            state = "London",
            postalCode = GB_POSTAL_CODE,
            phone = "+442079460123",
        )
    }
}
