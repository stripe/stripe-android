package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.CardBrand
import com.stripe.android.testharness.ViewTestUtils
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CardNumberEditText].
 */
@RunWith(RobolectricTestRunner::class)
class CardNumberEditTextTest {
    private var completionCallbackInvocations = 0
    private val completionCallback: () -> Unit = { completionCallbackInvocations++ }

    private var lastBrandChangeCallbackInvocation: CardBrand? = null
    private val brandChangeCallback: (CardBrand) -> Unit = {
        lastBrandChangeCallbackInvocation = it
    }

    private val cardNumberEditText = CardNumberEditText(
        ApplicationProvider.getApplicationContext()
    )

    @BeforeTest
    fun setup() {
        cardNumberEditText.setText("")
        cardNumberEditText.completionCallback = completionCallback
        cardNumberEditText.brandChangeCallback = brandChangeCallback
    }

    @Test
    fun updateSelectionIndex_whenVisa_increasesIndexWhenGoingPastTheSpaces() {
        // Directly setting card brand as a testing hack (annotated in source)
        cardNumberEditText.cardBrand = CardBrand.Visa

        // Adding 1 character, starting at position 4, with a final string length 6
        assertThat(
            cardNumberEditText.updateSelectionIndex(6, 4, 1)
        ).isEqualTo(6)
        assertThat(
            cardNumberEditText.updateSelectionIndex(8, 4, 1)
        ).isEqualTo(6)
        assertThat(
            cardNumberEditText.updateSelectionIndex(11, 9, 1)
        ).isEqualTo(11)
        assertThat(
            cardNumberEditText.updateSelectionIndex(16, 14, 1)
        ).isEqualTo(16)
    }

    @Test
    fun updateSelectionIndex_whenAmEx_increasesIndexWhenGoingPastTheSpaces() {
        cardNumberEditText.cardBrand = CardBrand.AmericanExpress
        assertThat(
            cardNumberEditText.updateSelectionIndex(6, 4, 1)
        ).isEqualTo(6)
        assertThat(
            cardNumberEditText.updateSelectionIndex(13, 11, 1)
        ).isEqualTo(13)
    }

    @Test
    fun updateSelectionIndex_whenDinersClub_decreasesIndexWhenDeletingPastTheSpaces() {
        cardNumberEditText.cardBrand = CardBrand.DinersClub

        assertThat(
            cardNumberEditText.updateSelectionIndex(6, 5, 0)
        ).isEqualTo(4)
        assertThat(
            cardNumberEditText.updateSelectionIndex(13, 10, 0)
        ).isEqualTo(9)
        assertThat(
            cardNumberEditText.updateSelectionIndex(17, 15, 0)
        ).isEqualTo(14)
    }

    @Test
    fun updateSelectionIndex_whenDeletingNotOnGaps_doesNotDecreaseIndex() {
        cardNumberEditText.cardBrand = CardBrand.DinersClub

        assertThat(
            cardNumberEditText.updateSelectionIndex(12, 7, 0)
        ).isEqualTo(7)
    }

    @Test
    fun updateSelectionIndex_whenAmEx_decreasesIndexWhenDeletingPastTheSpaces() {
        cardNumberEditText.cardBrand = CardBrand.AmericanExpress

        assertThat(
            cardNumberEditText.updateSelectionIndex(10, 5, 0)
        ).isEqualTo(4)
        assertThat(
            cardNumberEditText.updateSelectionIndex(13, 12, 0)
        ).isEqualTo(11)
    }

    @Test
    fun updateSelectionIndex_whenSelectionInTheMiddle_increasesIndexOverASpace() {
        cardNumberEditText.cardBrand = CardBrand.Visa
        assertThat(
            cardNumberEditText.updateSelectionIndex(10, 4, 1)
        ).isEqualTo(6)
    }

    @Test
    fun updateSelectionIndex_whenPastingIntoAGap_includesTheGapJump() {
        cardNumberEditText.cardBrand = CardBrand.Unknown

        assertThat(
            cardNumberEditText.updateSelectionIndex(12, 8, 2)
        ).isEqualTo(11)
    }

    @Test
    fun updateSelectionIndex_whenPastingOverAGap_includesTheGapJump() {
        cardNumberEditText.cardBrand = CardBrand.Unknown
        assertThat(
            cardNumberEditText.updateSelectionIndex(12, 3, 5)
        ).isEqualTo(9)
    }

    @Test
    fun updateSelectionIndex_whenIndexWouldGoOutOfBounds_setsToEndOfString() {
        cardNumberEditText.cardBrand = CardBrand.Visa

        // This case could happen when you paste over 5 digits with only 2
        assertThat(
            cardNumberEditText.updateSelectionIndex(3, 3, 2)
        ).isEqualTo(3)
    }

    @Test
    fun setText_whenTextIsValidCommonLengthNumber_changesCardValidState() {
        cardNumberEditText.setText(VISA_WITH_SPACES)

        assertThat(cardNumberEditText.isCardNumberValid)
            .isTrue()
        assertThat(completionCallbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun setText_whenTextIsSpacelessValidNumber_changesToSpaceNumberAndValidates() {
        cardNumberEditText.setText(VISA_NO_SPACES)

        assertThat(cardNumberEditText.isCardNumberValid)
            .isTrue()
        assertThat(completionCallbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun setText_whenTextIsValidAmExDinersClubLengthNumber_changesCardValidState() {
        cardNumberEditText.setText(AMEX_WITH_SPACES)

        assertThat(cardNumberEditText.isCardNumberValid)
            .isTrue()
        assertThat(completionCallbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun setText_whenTextChangesFromValidToInvalid_changesCardValidState() {
        cardNumberEditText.setText(VISA_WITH_SPACES)
        // Simply setting the value interacts with this mock once -- that is tested elsewhere
        completionCallbackInvocations = 0

        // Removing last character should make this invalid
        cardNumberEditText.setText(
            withoutLastCharacter(cardNumberEditText.text.toString())
        )

        assertThat(cardNumberEditText.isCardNumberValid)
            .isFalse()
        assertThat(completionCallbackInvocations)
            .isEqualTo(0)
    }

    @Test
    fun setText_whenTextIsInvalidCommonLengthNumber_doesNotNotifyListener() {
        // This creates a full-length but not valid number: 4242 4242 4242 4243
        cardNumberEditText.setText(
            withoutLastCharacter(VISA_WITH_SPACES) + "3"
        )

        assertThat(cardNumberEditText.isCardNumberValid)
            .isFalse()
        assertThat(completionCallbackInvocations)
            .isEqualTo(0)
    }

    @Test
    fun whenNotFinishedTyping_doesNotSetErrorValue() {
        // We definitely shouldn't start out in an error state.
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.setText("123")
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenValidCard_doesNotSetErrorValue() {
        cardNumberEditText.setText(withoutLastCharacter(VISA_WITH_SPACES))
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        // We now have the valid 4242 Visa
        cardNumberEditText.append("2")
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenInvalidCard_setsErrorValue() {
        cardNumberEditText.setText(withoutLastCharacter(VISA_WITH_SPACES))

        // This makes the number officially invalid
        cardNumberEditText.append("3")
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()
    }

    @Test
    fun finishTypingInvalidCardNumber_whenFollowedByDelete_setsErrorBackToFalse() {
        cardNumberEditText.setText(
            withoutLastCharacter(VISA_WITH_SPACES) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingDinersClub14_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        cardNumberEditText.setText(
            withoutLastCharacter(DINERS_CLUB_14_WITH_SPACES) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append(DINERS_CLUB_14_WITH_SPACES.last().toString())
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingDinersClub16_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        cardNumberEditText.setText(
            withoutLastCharacter(DINERS_CLUB_16_WITH_SPACES) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append(DINERS_CLUB_16_WITH_SPACES.last().toString())
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingAmEx_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        cardNumberEditText.setText(
            withoutLastCharacter(AMEX_WITH_SPACES) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append("5")
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun setCardBrandChangeListener_callsSetCardBrand() {
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterVisaBin_callsBrandListener() {
        cardNumberEditText.setText(CardNumberFixtures.VISA_BIN)
        assertEquals(CardBrand.Visa, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun addAmExBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.AmericanExpress, CardNumberFixtures.AMEX_BIN)
    }

    @Test
    fun addDinersClubBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.DinersClub, CardNumberFixtures.DINERS_CLUB_14_BIN)
        verifyCardBrandBin(CardBrand.DinersClub, CardNumberFixtures.DINERS_CLUB_16_BIN)
    }

    @Test
    fun addDiscoverBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.Discover, CardNumberFixtures.DISCOVER_BIN)
    }

    @Test
    fun addMasterCardBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.MasterCard, CardNumberFixtures.MASTERCARD_BIN)
    }

    @Test
    fun addJcbBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.JCB, CardNumberFixtures.JCB_BIN)
    }

    @Test
    fun enterCompleteNumberInParts_onlyCallsBrandListenerOnce() {
        cardNumberEditText.append(AMEX_WITH_SPACES.take(2))
        cardNumberEditText.append(AMEX_WITH_SPACES.drop(2))
        assertEquals(CardBrand.AmericanExpress, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterBrandBin_thenDelete_callsUpdateWithUnknown() {
        cardNumberEditText.setText(CardNumberFixtures.DINERS_CLUB_14_BIN)
        assertEquals(CardBrand.DinersClub, lastBrandChangeCallbackInvocation)

        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterBrandBin_thenClearAllText_callsUpdateWithUnknown() {
        cardNumberEditText.setText(CardNumberFixtures.VISA_BIN)
        assertEquals(CardBrand.Visa, lastBrandChangeCallbackInvocation)

        // Just adding some other text. Not enough to invalidate the card or complete it.
        lastBrandChangeCallbackInvocation = null
        cardNumberEditText.append("123")
        assertNull(lastBrandChangeCallbackInvocation)

        // This simulates the user selecting all text and deleting it.
        cardNumberEditText.setText("")

        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun cardNumber_withSpaces_returnsCardNumberWithoutSpaces() {
        cardNumberEditText.setText(VISA_WITH_SPACES)
        assertThat(cardNumberEditText.cardNumber)
            .isEqualTo(VISA_NO_SPACES)

        cardNumberEditText.setText("")
        cardNumberEditText.setText(AMEX_WITH_SPACES)
        assertThat(cardNumberEditText.cardNumber)
            .isEqualTo(AMEX_NO_SPACES)

        cardNumberEditText.setText("")
        cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)
        assertThat(cardNumberEditText.cardNumber)
            .isEqualTo(DINERS_CLUB_14_NO_SPACES)

        cardNumberEditText.setText("")
        cardNumberEditText.setText(DINERS_CLUB_16_WITH_SPACES)
        assertThat(cardNumberEditText.cardNumber)
            .isEqualTo(DINERS_CLUB_16_NO_SPACES)
    }

    @Test
    fun getCardNumber_whenIncompleteCard_returnsNull() {
        cardNumberEditText.setText(
            DINERS_CLUB_14_WITH_SPACES.take(DINERS_CLUB_14_WITH_SPACES.length - 2)
        )
        assertThat(cardNumberEditText.cardNumber)
            .isNull()
    }

    @Test
    fun getCardNumber_whenInvalidCardNumber_returnsNull() {
        cardNumberEditText.setText(
            withoutLastCharacter(VISA_WITH_SPACES) + "3" // creates the 4242 4242 4242 4243 bad number
        )
        assertThat(cardNumberEditText.cardNumber)
            .isNull()
    }

    @Test
    fun getCardNumber_whenValidNumberIsChangedToInvalid_returnsNull() {
        cardNumberEditText.setText(AMEX_WITH_SPACES)
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)

        assertThat(cardNumberEditText.cardNumber)
            .isNull()
    }

    @Test
    fun testUpdateCardBrandFromNumber() {
        cardNumberEditText.updateCardBrandFromNumber(BinFixtures.DINERSCLUB14)
        assertEquals(CardBrand.DinersClub, lastBrandChangeCallbackInvocation)
        cardNumberEditText.updateCardBrandFromNumber(BinFixtures.AMEX)
        assertEquals(CardBrand.AmericanExpress, lastBrandChangeCallbackInvocation)
    }

    private fun verifyCardBrandBin(
        cardBrand: CardBrand,
        bin: String
    ) {
        // Reset inside the loop so we don't count each prefix
        lastBrandChangeCallbackInvocation = null
        cardNumberEditText.setText(bin)
        assertEquals(cardBrand, lastBrandChangeCallbackInvocation)
        cardNumberEditText.setText("")
    }

    private companion object {
        private fun withoutLastCharacter(s: String) = s.take(s.length - 1)
    }
}
