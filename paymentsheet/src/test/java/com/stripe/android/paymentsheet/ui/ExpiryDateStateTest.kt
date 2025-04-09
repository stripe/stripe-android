package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.Test

internal class ExpiryDateStateTest {

    @Test
    fun `create should properly format the expiry date when fields are valid`() {
        val card = createCard(expiryMonth = 12, expiryYear = 2045)

        val state = ExpiryDateState.create(card, enabled = true)

        assertThat(state.text).isEqualTo("1245")
        assertThat(state.expiryMonth).isEqualTo(12)
        assertThat(state.expiryYear).isEqualTo(2045)
    }

    @Test
    fun `create should use fallback date for invalid dates when not enabled`() {
        val card = createCard(expiryMonth = null, expiryYear = 2023)

        val state = ExpiryDateState.create(card, enabled = false)

        assertThat(state.text).isEqualTo(CARD_EDIT_UI_FALLBACK_EXPIRY_DATE)
        assertThat(state.expiryMonth).isNull()
        assertThat(state.expiryYear).isNull()
    }

    @Test
    fun `create should format the month with leading zero when needed`() {
        val card = createCard(expiryMonth = 3, expiryYear = 2045)

        val state = ExpiryDateState.create(card, enabled = true)

        assertThat(state.text).isEqualTo("0345")
        assertThat(state.expiryMonth).isEqualTo(3)
        assertThat(state.expiryYear).isEqualTo(2045)
    }

    @Test
    fun `create should handle double-digit months without leading zero`() {
        val card = createCard(expiryMonth = 10, expiryYear = 2045)

        val state = ExpiryDateState.create(card, enabled = true)

        assertThat(state.text).isEqualTo("1045")
        assertThat(state.expiryMonth).isEqualTo(10)
        assertThat(state.expiryYear).isEqualTo(2045)
    }

    @Test
    fun `shouldShowError should return false for valid expiry date`() {
        val state = ExpiryDateState(text = VALID_EXPIRY_TEXT, enabled = true)

        assertThat(state.shouldShowError()).isFalse()
    }

    @Test
    fun `shouldShowError should return true for expired date`() {
        val state = ExpiryDateState(text = EXPIRED_EXPIRY_TEXT, enabled = true)

        assertThat(state.shouldShowError()).isTrue()
    }

    @Test
    fun `sectionError should return null for valid date`() {
        val state = ExpiryDateState(text = VALID_EXPIRY_TEXT, enabled = true)

        assertThat(state.sectionError()).isNull()
    }

    @Test
    fun `sectionError should return null when not enabled even if invalid`() {
        val state = ExpiryDateState(text = INVALID_FORMAT_EXPIRY_TEXT, enabled = false)

        assertThat(state.sectionError()).isNull()
    }

    @Test
    fun `onDateChanged should accept valid input`() {
        val state = ExpiryDateState(text = "12", enabled = true)
        val newState = state.onDateChanged("1223")

        assertThat(newState.text).isEqualTo("1223")
    }

    @Test
    fun `onDateChanged should not accept input when field is full`() {
        val state = ExpiryDateState(text = VALID_EXPIRY_TEXT, enabled = true)
        val newState = state.onDateChanged("12235") // Trying to add an extra digit

        // Should not change the text since it's already full (4 digits)
        assertThat(newState.text).isEqualTo(VALID_EXPIRY_TEXT)
    }

    @Test
    fun `onDateChanged should accept input when deleting characters`() {
        val state = ExpiryDateState(text = VALID_EXPIRY_TEXT, enabled = true)
        val newState = state.onDateChanged("122") // Deleting last digit

        assertThat(newState.text).isEqualTo("122")
    }

    @Test
    fun `expiryMonth should return null for invalid text`() {
        val state = ExpiryDateState(text = CARD_EDIT_UI_FALLBACK_EXPIRY_DATE, enabled = false)

        assertThat(state.expiryMonth).isNull()
    }

    @Test
    fun `expiryYear should return null for invalid text`() {
        val state = ExpiryDateState(text = CARD_EDIT_UI_FALLBACK_EXPIRY_DATE, enabled = false)

        assertThat(state.expiryYear).isNull()
    }

    @Test
    fun `expiryMonth should extract month correctly from valid text`() {
        val state = ExpiryDateState(text = "0525", enabled = true)

        assertThat(state.expiryMonth).isEqualTo(5)
    }

    @Test
    fun `expiryYear should extract year correctly from valid text`() {
        val state = ExpiryDateState(text = "0525", enabled = true)

        assertThat(state.expiryYear).isEqualTo(2025)
    }

    private fun createCard(expiryMonth: Int?, expiryYear: Int?): PaymentMethod.Card {
        return PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
            expiryMonth = expiryMonth,
            expiryYear = expiryYear
        )
    }
}

private const val VALID_EXPIRY_TEXT = "1245"
private const val INVALID_FORMAT_EXPIRY_TEXT = "123"
private const val EXPIRED_EXPIRY_TEXT = "0120"
