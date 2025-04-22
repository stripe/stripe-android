package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures.BILLING_DETAILS_FORM_DETAILS
import com.stripe.android.paymentsheet.PaymentSheetFixtures.billingDetailsFormState
import com.stripe.android.uicore.forms.FormFieldEntry
import org.junit.Test

internal class BillingDetailsEntryTest {
    @Test
    fun `isComplete() returns true when all required fields are complete in Full mode`() {
        val state = billingDetailsEntry()

        assertThat(state.isComplete(AddressCollectionMode.Full)).isTrue()
    }

    @Test
    fun `isComplete() returns false when required fields are incomplete in Full mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                line1 = FormFieldEntry("", isComplete = false),
            )
        )

        assertThat(state.isComplete(AddressCollectionMode.Full)).isFalse()
    }

    @Test
    fun `isComplete() returns true when only postal code and country are present in Automatic mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                line1 = null,
                line2 = null,
                city = null,
                state = null,
            )
        )

        assertThat(state.isComplete(AddressCollectionMode.Automatic)).isTrue()
    }

    @Test
    fun `isComplete() returns false when postal code is incomplete in Automatic mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                postalCode = FormFieldEntry("", isComplete = false),
                country = FormFieldEntry("US", isComplete = true),
            )
        )

        assertThat(state.isComplete(AddressCollectionMode.Automatic)).isFalse()
    }

    @Test
    fun `isComplete() returns false when country is incomplete in Automatic mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = FormFieldEntry("94107", isComplete = true),
                country = FormFieldEntry("", isComplete = false),
            )
        )

        assertThat(state.isComplete(AddressCollectionMode.Automatic)).isFalse()
    }

    @Test
    fun `isComplete() always returns true in Never mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        assertThat(state.isComplete(AddressCollectionMode.Never)).isTrue()
    }

    @Test
    fun `hasChanged() returns true when postal code changes in Automatic mode`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.copy(
            address = Address(
                postalCode = "94107",
            )
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                postalCode = FormFieldEntry("94108", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            addressCollectionMode = AddressCollectionMode.Automatic
        )
        assertThat(hasChanged).isTrue()
    }

    @Test
    fun `hasChanged() returns true when country changes in Automatic mode`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.copy(
            address = Address(
                country = "US"
            )
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                country = FormFieldEntry("CA", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            addressCollectionMode = AddressCollectionMode.Automatic
        )
        assertThat(hasChanged).isTrue()
    }

    @Test
    fun `hasChanged() returns false when values are the same in Automatic mode`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.copy(
            address = Address(
                postalCode = "94107",
                country = "US"
            )
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                postalCode = FormFieldEntry("94107", isComplete = true),
                country = FormFieldEntry("US", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            addressCollectionMode = AddressCollectionMode.Automatic
        )
        assertThat(hasChanged).isFalse()
    }

    @Test
    fun `hasChanged() returns true when any field changes in Full mode`() {
        val addressFields = listOf(
            billingDetailsEntry(
                billingDetailsFormState = billingDetailsFormState(
                    line1 = FormFieldEntry("456 Main St", isComplete = true),
                )
            ),
            billingDetailsEntry(
                billingDetailsFormState = billingDetailsFormState(
                    line2 = FormFieldEntry("Suite 100", isComplete = true),
                )
            ),
            billingDetailsEntry(
                billingDetailsFormState = billingDetailsFormState(
                    city = FormFieldEntry("Oakland", isComplete = true),
                )
            ),
            billingDetailsEntry(
                billingDetailsFormState = billingDetailsFormState(
                    state = FormFieldEntry("NY", isComplete = true),
                )
            )
        )

        addressFields.forEach { state ->
            val hasChanged = state.hasChanged(
                billingDetails = BILLING_DETAILS_FORM_DETAILS,
                addressCollectionMode = AddressCollectionMode.Full
            )
            assertThat(hasChanged).isTrue()
        }
    }

    @Test
    fun `hasChanged() returns false when all fields are the same in Full mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState()
        )

        val hasChanged = state.hasChanged(
            billingDetails = BILLING_DETAILS_FORM_DETAILS,
            addressCollectionMode = AddressCollectionMode.Full
        )
        assertThat(hasChanged).isFalse()
    }

    @Test
    fun `hasChanged() always returns false in Never mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                postalCode = FormFieldEntry("10001", isComplete = true),
                country = FormFieldEntry("NY", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = BILLING_DETAILS_FORM_DETAILS,
            addressCollectionMode = AddressCollectionMode.Never
        )
        assertThat(hasChanged).isFalse()
    }

    @Test
    fun `hasChanged() handles null original billingDetails`() {
        val state = billingDetailsEntry()

        // Should detect changes when starting with null billing details
        val hasChanged = state.hasChanged(
            billingDetails = null,
            addressCollectionMode = AddressCollectionMode.Full
        )
        assertThat(hasChanged).isTrue()
    }

    @Test
    fun `hasChanged() handles null vs empty string in address fields`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.copy(
            address = BILLING_DETAILS_FORM_DETAILS.address?.copy(
                line2 = null,
                state = null,
            )
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                line2 = FormFieldEntry("", isComplete = true),
                state = FormFieldEntry("", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            addressCollectionMode = AddressCollectionMode.Full
        )
        assertThat(hasChanged).isFalse()
    }

    private fun billingDetailsEntry(
        billingDetailsFormState: BillingDetailsFormState = billingDetailsFormState()
    ): BillingDetailsEntry {
        return BillingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState
        )
    }
}
