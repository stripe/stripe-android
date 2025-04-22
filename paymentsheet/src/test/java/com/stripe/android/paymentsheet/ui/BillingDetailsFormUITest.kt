package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures.BILLING_DETAILS_FORM_DETAILS
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.paymentelementtestpages.BillingDetailsPage
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BillingDetailsFormUITest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun requiredFieldsAreVisibleForAutomaticCollectionMode() = runScenario {
        page.country.assertIsDisplayed()
        page.line1.assertDoesNotExist()
        page.line2.assertDoesNotExist()
        page.city.assertDoesNotExist()
        page.state.assertDoesNotExist()
        page.zipCode.assertIsDisplayed()
    }

    @Test
    fun requiredFieldsAreVisibleForFullCollectionMode() = runScenario(
        addressCollectionMode = AddressCollectionMode.Full,
    ) {
        page.country.assertIsDisplayed()
        page.line1.assertIsDisplayed()
        page.line2.assertIsDisplayed()
        page.city.assertIsDisplayed()
        page.state.assertIsDisplayed()
        page.zipCode.assertIsDisplayed()
    }

    @Test
    fun formInputChangesProduceNewBillingDetailsFormState() {
        var billingDetailsFormState: BillingDetailsFormState? = null
        runScenario(
            addressCollectionMode = AddressCollectionMode.Full,
            onValuesChanged = {
                billingDetailsFormState = it
            }
        ) {
            page.zipCode.performTextReplacement("94100")
            page.city.performTextReplacement("San Diego")
            page.line1.performTextReplacement("1 Fake St")
            page.line2.performTextReplacement("Suite 100")

            assertThat(billingDetailsFormState?.postalCode?.value).isEqualTo("94100")
            assertThat(billingDetailsFormState?.city?.value).isEqualTo("San Diego")
            assertThat(billingDetailsFormState?.line1?.value).isEqualTo("1 Fake St")
            assertThat(billingDetailsFormState?.line2?.value).isEqualTo("Suite 100")
        }
    }

    private fun runScenario(
        billingDetails: PaymentMethod.BillingDetails = BILLING_DETAILS_FORM_DETAILS,
        addressCollectionMode: AddressCollectionMode = AddressCollectionMode.Automatic,
        onValuesChanged: (BillingDetailsFormState) -> Unit = {},
        block: TestScenario.() -> Unit
    ) {
        val form = BillingDetailsForm(
            billingDetails = billingDetails,
            addressCollectionMode = addressCollectionMode
        )
        composeRule.setContent {
            BillingDetailsFormUI(
                form = form,
                onValuesChanged = onValuesChanged
            )
        }
        block(TestScenario(form, BillingDetailsPage(composeRule)))
    }

    data class TestScenario(
        val form: BillingDetailsForm,
        val page: BillingDetailsPage,
    )
}
