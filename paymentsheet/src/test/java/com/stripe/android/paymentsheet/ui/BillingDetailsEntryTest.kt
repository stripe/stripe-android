package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.PaymentSheetFixtures.BILLING_DETAILS_FORM_DETAILS
import com.stripe.android.paymentsheet.PaymentSheetFixtures.billingDetailsFormState
import com.stripe.android.uicore.forms.FormFieldEntry
import org.junit.Test

internal class BillingDetailsEntryTest {

    private val fullAddressConfig = BillingDetailsCollectionConfiguration(
        address = AddressCollectionMode.Full
    )

    private val automaticAddressConfig = BillingDetailsCollectionConfiguration(
        address = AddressCollectionMode.Automatic
    )

    private val neverAddressConfig = BillingDetailsCollectionConfiguration(
        address = AddressCollectionMode.Never
    )

    @Test
    fun `isComplete() returns true when all required fields are complete in Full mode`() {
        val state = billingDetailsEntry()

        assertThat(state.isComplete(fullAddressConfig)).isTrue()
    }

    @Test
    fun `isComplete() returns false when required fields are incomplete in Full mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                line1 = FormFieldEntry("", isComplete = false),
            )
        )

        assertThat(state.isComplete(fullAddressConfig)).isFalse()
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

        assertThat(state.isComplete(automaticAddressConfig)).isTrue()
    }

    @Test
    fun `isComplete() returns false when postal code is incomplete in Automatic mode`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                postalCode = FormFieldEntry("", isComplete = false),
                country = FormFieldEntry("US", isComplete = true),
            )
        )

        assertThat(state.isComplete(automaticAddressConfig)).isFalse()
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

        assertThat(state.isComplete(automaticAddressConfig)).isFalse()
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

        assertThat(state.isComplete(neverAddressConfig)).isTrue()
    }

    @Test
    fun `hasChanged() returns true when postal code changes in Automatic mode`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.toBuilder().setAddress(
            address = Address(
                postalCode = "94107",
            )
        ).build()

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                postalCode = FormFieldEntry("94108", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            billingDetailsCollectionConfiguration = automaticAddressConfig
        )
        assertThat(hasChanged).isTrue()
    }

    @Test
    fun `hasChanged() returns true when country changes in Automatic mode`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.toBuilder().setAddress(
            address = Address(
                country = "US"
            )
        ).build()

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                country = FormFieldEntry("CA", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            billingDetailsCollectionConfiguration = automaticAddressConfig
        )
        assertThat(hasChanged).isTrue()
    }

    @Test
    fun `hasChanged() returns false when values are the same in Automatic mode`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.toBuilder().setAddress(
            address = Address(
                postalCode = "94107",
                country = "US"
            )
        ).build()

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                postalCode = FormFieldEntry("94107", isComplete = true),
                country = FormFieldEntry("US", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            billingDetailsCollectionConfiguration = automaticAddressConfig
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
                billingDetailsCollectionConfiguration = fullAddressConfig
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
            billingDetailsCollectionConfiguration = fullAddressConfig
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
            billingDetailsCollectionConfiguration = neverAddressConfig
        )
        assertThat(hasChanged).isFalse()
    }

    @Test
    fun `hasChanged() handles null original billingDetails`() {
        val state = billingDetailsEntry()

        // Should detect changes when starting with null billing details
        val hasChanged = state.hasChanged(
            billingDetails = null,
            billingDetailsCollectionConfiguration = fullAddressConfig
        )
        assertThat(hasChanged).isTrue()
    }

    @Test
    fun `hasChanged() handles null vs empty string in address fields`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.toBuilder().setAddress(
            address = BILLING_DETAILS_FORM_DETAILS.address?.copy(
                line2 = null,
                state = null,
            )
        ).build()

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                line2 = FormFieldEntry("", isComplete = true),
                state = FormFieldEntry("", isComplete = true),
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            billingDetailsCollectionConfiguration = fullAddressConfig
        )
        assertThat(hasChanged).isFalse()
    }

    @Test
    fun `hasChanged() returns false when comparing String against null FormFieldEntry`() {
        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                line1 = null,
                line2 = null,
                city = null,
                state = null,
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = BILLING_DETAILS_FORM_DETAILS,
            billingDetailsCollectionConfiguration = automaticAddressConfig
        )
        assertThat(hasChanged).isFalse()
    }

    @Test
    fun `isComplete() returns true when name is collected and complete`() {
        val configWithName = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                name = FormFieldEntry("John Doe", isComplete = true),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        assertThat(state.isComplete(configWithName)).isTrue()
    }

    @Test
    fun `isComplete() returns false when name is collected but incomplete`() {
        val configWithName = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                name = FormFieldEntry("", isComplete = false),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        assertThat(state.isComplete(configWithName)).isFalse()
    }

    @Test
    fun `isComplete() returns true when email is collected and complete`() {
        val configWithEmail = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                email = FormFieldEntry("john@example.com", isComplete = true),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        assertThat(state.isComplete(configWithEmail)).isTrue()
    }

    @Test
    fun `isComplete() returns false when email is collected but incomplete`() {
        val configWithEmail = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                email = FormFieldEntry("invalid-email", isComplete = false),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        assertThat(state.isComplete(configWithEmail)).isFalse()
    }

    @Test
    fun `isComplete() returns true when phone is collected and complete`() {
        val configWithPhone = BillingDetailsCollectionConfiguration(
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                phone = FormFieldEntry("+1234567890", isComplete = true),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        assertThat(state.isComplete(configWithPhone)).isTrue()
    }

    @Test
    fun `isComplete() returns false when phone is collected but incomplete`() {
        val configWithPhone = BillingDetailsCollectionConfiguration(
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                phone = FormFieldEntry("123", isComplete = false),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        assertThat(state.isComplete(configWithPhone)).isFalse()
    }

    @Test
    fun `hasChanged() returns true when name changes`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.toBuilder().setName(
            name = "Original Name"
        ).build()

        val configWithName = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                name = FormFieldEntry("New Name", isComplete = true),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            billingDetailsCollectionConfiguration = configWithName
        )
        assertThat(hasChanged).isTrue()
    }

    @Test
    fun `hasChanged() returns true when email changes`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.toBuilder().setEmail(
            email = "original@example.com"
        ).build()

        val configWithEmail = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                email = FormFieldEntry("new@example.com", isComplete = true),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            billingDetailsCollectionConfiguration = configWithEmail
        )
        assertThat(hasChanged).isTrue()
    }

    @Test
    fun `hasChanged() returns true when phone changes`() {
        val billingDetails = BILLING_DETAILS_FORM_DETAILS.toBuilder().setPhone(
            phone = "+1234567890"
        ).build()

        val configWithPhone = BillingDetailsCollectionConfiguration(
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Never
        )

        val state = billingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState(
                phone = FormFieldEntry("+0987654321", isComplete = true),
                line1 = null,
                line2 = null,
                city = null,
                state = null,
                postalCode = null,
                country = null,
            )
        )

        val hasChanged = state.hasChanged(
            billingDetails = billingDetails,
            billingDetailsCollectionConfiguration = configWithPhone
        )
        assertThat(hasChanged).isTrue()
    }

    private fun billingDetailsEntry(
        billingDetailsFormState: BillingDetailsFormState = billingDetailsFormState()
    ): BillingDetailsEntry {
        return BillingDetailsEntry(
            billingDetailsFormState = billingDetailsFormState
        )
    }
}
