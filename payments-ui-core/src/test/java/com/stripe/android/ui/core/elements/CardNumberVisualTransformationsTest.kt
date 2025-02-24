package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.CardNumberFixtures
import org.junit.Test

class CardNumberVisualTransformationsTest {
    @Test
    fun `For the default transformation, card numbers are formatted correctly`() {
        val transformation = CardNumberVisualTransformations.Default(separator = ' ')

        assertThat(transformation.filter(AnnotatedString(CardNumberFixtures.VISA_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.VISA_WITH_SPACES))

        assertThat(transformation.filter(AnnotatedString(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.DINERS_CLUB_16_WITH_SPACES))

        assertThat(transformation.filter(AnnotatedString(CardNumberFixtures.DISCOVER_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.DISCOVER_WITH_SPACES))

        assertThat(transformation.filter(AnnotatedString(CardNumberFixtures.JCB_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.JCB_WITH_SPACES))

        assertThat(transformation.filter(AnnotatedString(CardNumberFixtures.UNIONPAY_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.UNIONPAY_WITH_SPACES))
    }

    @Test
    fun `For the 14 and 15 PAN transformation, card numbers are formatted correctly`() {
        val transformation = CardNumberVisualTransformations.FourteenAndFifteenPanLength(separator = ' ')

        assertThat(transformation.filter(AnnotatedString(CardNumberFixtures.AMEX_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.AMEX_WITH_SPACES))

        assertThat(transformation.filter(AnnotatedString(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES))
    }

    @Test
    fun `For the 19 PAN transformation, card numbers are formatted correctly`() {
        val transformation = CardNumberVisualTransformations.NineteenPanLength(separator = ' ')

        assertThat(transformation.filter(AnnotatedString(CardNumberFixtures.UNIONPAY_19_NO_SPACES)).text)
            .isEqualTo(AnnotatedString(CardNumberFixtures.UNIONPAY_19_WITH_SPACES))
    }
}
