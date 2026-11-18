package com.stripe.android.paymentsheet.ui

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultEditCardDetailsInteractorAutomaticTaxTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `automatic tax requires a complete street address`() = assertIncompleteTaxAddress(
        PaymentSheetFixtures.billingDetailsFormState(line1 = FormFieldEntry("", isComplete = false))
    )

    @Test
    fun `automatic tax requires a complete city`() = assertIncompleteTaxAddress(
        PaymentSheetFixtures.billingDetailsFormState(city = FormFieldEntry("", isComplete = false))
    )

    @Test
    fun `automatic tax requires a complete state`() = assertIncompleteTaxAddress(
        PaymentSheetFixtures.billingDetailsFormState(state = FormFieldEntry("", isComplete = false))
    )

    @Test
    fun `automatic tax accepts complete address fields`() = runScenario {
        interactor.updateBillingDetails(
            PaymentSheetFixtures.billingDetailsFormState(
                postalCode = FormFieldEntry("94103", isComplete = true),
            )
        )

        assertThat(updates.awaitItem()?.billingDetails?.address?.postalCode).isEqualTo("94103")
    }

    @Test
    fun `automatic tax accepts country only when other fields are hidden`() = runScenario {
        interactor.updateBillingDetails(
            PaymentSheetFixtures.billingDetailsFormState(
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = FormFieldEntry("FR", isComplete = true),
            )
        )

        val address = updates.awaitItem()?.billingDetails?.address
        assertThat(address?.country).isEqualTo("FR")
        assertThat(address?.line1).isNull()
        assertThat(address?.city).isNull()
        assertThat(address?.state).isNull()
        assertThat(address?.postalCode).isNull()
    }

    @Test
    fun `automatic tax detects street edits without changing country or postal code`() = runScenario {
        val address = checkNotNull(PaymentMethodFixtures.BILLING_DETAILS.address)
        interactor.updateBillingDetails(
            PaymentSheetFixtures.billingDetailsFormState(
                line1 = FormFieldEntry("510 Townsend St", isComplete = true),
                line2 = address.line2?.let { FormFieldEntry(it, isComplete = true) },
                city = FormFieldEntry(address.city, isComplete = true),
                state = FormFieldEntry(address.state, isComplete = true),
                postalCode = FormFieldEntry(address.postalCode, isComplete = true),
                country = FormFieldEntry(address.country, isComplete = true),
            )
        )

        val updatedAddress = updates.awaitItem()?.billingDetails?.address
        assertThat(updatedAddress?.line1).isEqualTo("510 Townsend St")
        assertThat(updatedAddress?.country).isEqualTo(address.country)
        assertThat(updatedAddress?.postalCode).isEqualTo(address.postalCode)
    }

    @Test
    fun `automatic tax preserves Never address collection`() = runScenario(
        addressCollectionMode = AddressCollectionMode.Never,
    ) {
        assertThat(interactor.state.value.billingDetailsForm).isNull()
        interactor.updateBillingDetails(
            PaymentSheetFixtures.billingDetailsFormState(
                postalCode = FormFieldEntry("94103", isComplete = true),
            )
        )

        assertThat(updates.awaitItem()).isNull()
    }

    private fun assertIncompleteTaxAddress(formState: BillingDetailsFormState) = runScenario {
        interactor.updateBillingDetails(formState)

        assertThat(updates.awaitItem()).isNull()
    }

    private fun runScenario(
        addressCollectionMode: AddressCollectionMode = AddressCollectionMode.Automatic,
        block: suspend Scenario.() -> Unit,
    ) = runTest(testDispatcher) {
        val updates = Turbine<CardUpdateParams?>()
        val interactor = DefaultEditCardDetailsInteractor.Factory().create(
            coroutineScope = backgroundScope,
            cardEditConfiguration = CardEditConfiguration(
                cardBrandFilter = DefaultCardBrandFilter,
                isCbcModifiable = false,
                areExpiryDateAndAddressModificationSupported = true,
            ),
            requiresModification = true,
            payload = EditCardPayload.create(
                card = PaymentMethodFixtures.CARD,
                billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            ),
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                address = addressCollectionMode,
            ),
            requiresBillingAddressForAutomaticTax = true,
            onBrandChoiceChanged = { error("Not expected") },
            onCardUpdateParamsChanged = updates::add,
            autocompleteAddressInteractorFactory = null,
        )
        assertThat(updates.awaitItem()).isNull()

        Scenario(interactor, updates).block()
        updates.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val interactor: EditCardDetailsInteractor,
        val updates: Turbine<CardUpdateParams?>,
    )
}
