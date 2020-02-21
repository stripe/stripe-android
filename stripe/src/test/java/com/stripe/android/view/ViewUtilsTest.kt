package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.CardBrand
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [ViewUtils]
 */
@RunWith(RobolectricTestRunner::class)
internal class ViewUtilsTest {

    @Test
    fun separateCardNumberGroups_withVisaDebit_returnsCorrectCardGroups() {
        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES,
                CardBrand.Visa
            )
        ).isEqualTo(
            arrayOf("4000", "0566", "5566", "5556")
        )
    }

    @Test
    fun separateCardNumberGroups_withAmex_returnsCorrectCardGroups() {
        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.AMEX_NO_SPACES,
                CardBrand.AmericanExpress
            )
        ).isEqualTo(
            arrayOf("3782", "822463", "10005")
        )
    }

    @Test
    fun separateCardNumberGroups_withDinersClub14_returnsCorrectCardGroups() {
        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.DINERS_CLUB_14_NO_SPACES,
                CardBrand.DinersClub
            )
        ).isEqualTo(
            arrayOf("3622", "7206", "2716", "67")
        )
    }

    @Test
    fun separateCardNumberGroups_withInvalid_returnsCorrectCardGroups() {
        assertThat(
            ViewUtils.separateCardNumberGroups(
                "1234056655665556",
                CardBrand.Unknown
            )
        ).isEqualTo(
            arrayOf("1234", "0566", "5566", "5556")
        )
    }

    @Test
    fun separateCardNumberGroups_withAmexPrefix_returnsPrefixGroups() {
        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.AMEX_NO_SPACES.take(2),
                CardBrand.AmericanExpress
            )
        ).isEqualTo(
            arrayOf("37", null, null)
        )

        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.AMEX_NO_SPACES.take(5),
                CardBrand.AmericanExpress
            )
        ).isEqualTo(
            arrayOf("3782", "8", null)
        )

        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.AMEX_NO_SPACES.take(11),
                CardBrand.AmericanExpress
            )
        ).isEqualTo(
            arrayOf("3782", "822463", "1")
        )
    }

    @Test
    fun separateCardNumberGroups_withVisaPrefix_returnsCorrectGroups() {
        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES.take(2),
                CardBrand.Visa
            )
        ).isEqualTo(
            arrayOf("40", null, null, null)
        )

        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES.take(5),
                CardBrand.Visa
            )
        ).isEqualTo(
            arrayOf("4000", "0", null, null)
        )

        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES.take(9),
                CardBrand.Visa
            )
        ).isEqualTo(
            arrayOf("4000", "0566", "5", null)
        )

        assertThat(
            ViewUtils.separateCardNumberGroups(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES.take(15),
                CardBrand.Visa
            )
        ).isEqualTo(
            arrayOf("4000", "0566", "5566", "555")
        )
    }

    @Test
    fun separateCardNumberGroups_forLongInputs_doesNotCrash() {
        assertThat(
            ViewUtils.separateCardNumberGroups("1234567890123456789", CardBrand.Visa)
        ).hasLength(4)
    }
}
