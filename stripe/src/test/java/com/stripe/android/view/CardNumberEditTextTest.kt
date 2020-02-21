package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.model.CardBrand
import com.stripe.android.testharness.ViewTestUtils
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    private val cardNumberEditText: CardNumberEditText by lazy {
        CardNumberEditText(ApplicationProvider.getApplicationContext<Context>())
    }

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
        assertEquals(6, cardNumberEditText.updateSelectionIndex(6, 4, 1))
        assertEquals(6, cardNumberEditText.updateSelectionIndex(8, 4, 1))
        assertEquals(11, cardNumberEditText.updateSelectionIndex(11, 9, 1))
        assertEquals(16, cardNumberEditText.updateSelectionIndex(16, 14, 1))
    }

    @Test
    fun updateSelectionIndex_whenAmEx_increasesIndexWhenGoingPastTheSpaces() {
        cardNumberEditText.cardBrand = CardBrand.AmericanExpress
        assertEquals(6, cardNumberEditText.updateSelectionIndex(6, 4, 1))
        assertEquals(13, cardNumberEditText.updateSelectionIndex(13, 11, 1))
    }

    @Test
    fun updateSelectionIndex_whenDinersClub_decreasesIndexWhenDeletingPastTheSpaces() {
        cardNumberEditText.cardBrand = CardBrand.DinersClub
        assertEquals(4, cardNumberEditText.updateSelectionIndex(6, 5, 0))
        assertEquals(9, cardNumberEditText.updateSelectionIndex(13, 10, 0))
        assertEquals(14, cardNumberEditText.updateSelectionIndex(17, 15, 0))
    }

    @Test
    fun updateSelectionIndex_whenDeletingNotOnGaps_doesNotDecreaseIndex() {
        cardNumberEditText.cardBrand = CardBrand.DinersClub
        assertEquals(7, cardNumberEditText.updateSelectionIndex(12, 7, 0))
    }

    @Test
    fun updateSelectionIndex_whenAmEx_decreasesIndexWhenDeletingPastTheSpaces() {
        cardNumberEditText.cardBrand = CardBrand.AmericanExpress
        assertEquals(4, cardNumberEditText.updateSelectionIndex(10, 5, 0))
        assertEquals(11, cardNumberEditText.updateSelectionIndex(13, 12, 0))
    }

    @Test
    fun updateSelectionIndex_whenSelectionInTheMiddle_increasesIndexOverASpace() {
        cardNumberEditText.cardBrand = CardBrand.Visa
        assertEquals(6, cardNumberEditText.updateSelectionIndex(10, 4, 1))
    }

    @Test
    fun updateSelectionIndex_whenPastingIntoAGap_includesTheGapJump() {
        cardNumberEditText.cardBrand = CardBrand.Unknown
        assertEquals(11, cardNumberEditText.updateSelectionIndex(12, 8, 2))
    }

    @Test
    fun updateSelectionIndex_whenPastingOverAGap_includesTheGapJump() {
        cardNumberEditText.cardBrand = CardBrand.Unknown
        assertEquals(9, cardNumberEditText.updateSelectionIndex(12, 3, 5))
    }

    @Test
    fun updateSelectionIndex_whenIndexWouldGoOutOfBounds_setsToEndOfString() {
        cardNumberEditText.cardBrand = CardBrand.Visa
        // This case could happen when you paste over 5 digits with only 2
        assertEquals(3, cardNumberEditText.updateSelectionIndex(3, 3, 2))
    }

    @Test
    fun setText_whenTextIsValidCommonLengthNumber_changesCardValidState() {
        cardNumberEditText.setText(VISA_WITH_SPACES)

        assertTrue(cardNumberEditText.isCardNumberValid)
        assertEquals(1, completionCallbackInvocations)
    }

    @Test
    fun setText_whenTextIsSpacelessValidNumber_changesToSpaceNumberAndValidates() {
        cardNumberEditText.setText(VISA_NO_SPACES)

        assertTrue(cardNumberEditText.isCardNumberValid)
        assertEquals(1, completionCallbackInvocations)
    }

    @Test
    fun setText_whenTextIsValidAmExDinersClubLengthNumber_changesCardValidState() {
        cardNumberEditText.setText(AMEX_WITH_SPACES)

        assertTrue(cardNumberEditText.isCardNumberValid)
        assertEquals(1, completionCallbackInvocations)
    }

    @Test
    fun setText_whenTextChangesFromValidToInvalid_changesCardValidState() {
        cardNumberEditText.setText(VISA_WITH_SPACES)
        // Simply setting the value interacts with this mock once -- that is tested elsewhere
        completionCallbackInvocations = 0

        var mutable = cardNumberEditText.text.toString()
        // Removing a single character should make this invalid
        mutable = mutable.substring(0, 18)
        cardNumberEditText.setText(mutable)
        assertFalse(cardNumberEditText.isCardNumberValid)
        assertEquals(0, completionCallbackInvocations)
    }

    @Test
    fun setText_whenTextIsInvalidCommonLengthNumber_doesNotNotifyListener() {
        // This creates a full-length but not valid number: 4242 4242 4242 4243
        val almostValid = VISA_WITH_SPACES.substring(0, 18) + "3"
        cardNumberEditText.setText(almostValid)
        assertFalse(cardNumberEditText.isCardNumberValid)
        assertEquals(0, completionCallbackInvocations)
    }

    @Test
    fun whenNotFinishedTyping_doesNotSetErrorValue() {
        // We definitely shouldn't start out in an error state.
        assertFalse(cardNumberEditText.shouldShowError)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "123")
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenValidCard_doesNotSetErrorValue() {
        val almostThere = VISA_WITH_SPACES.substring(0, 18)
        cardNumberEditText.setText(almostThere)
        assertFalse(cardNumberEditText.shouldShowError)
        // We now have the valid 4242 Visa
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "2")
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenInvalidCard_setsErrorValue() {
        val almostThere = VISA_WITH_SPACES.substring(0, 18)
        cardNumberEditText.setText(almostThere)
        // This makes the number officially invalid
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "3")
        assertTrue(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingInvalidCardNumber_whenFollowedByDelete_setsErrorBackToFalse() {
        val notQuiteValid = VISA_WITH_SPACES.substring(0, 18) + "3"
        cardNumberEditText.setText(notQuiteValid)
        assertTrue(cardNumberEditText.shouldShowError)

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingDinersClub14_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        val notQuiteValid = DINERS_CLUB_14_WITH_SPACES.take(DINERS_CLUB_14_WITH_SPACES.length - 1) + "3"
        cardNumberEditText.setText(notQuiteValid)
        assertTrue(cardNumberEditText.shouldShowError)

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertFalse(cardNumberEditText.shouldShowError)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + DINERS_CLUB_14_WITH_SPACES.last())
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingDinersClub16_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        val notQuiteValid = DINERS_CLUB_16_WITH_SPACES.take(DINERS_CLUB_16_WITH_SPACES.length - 1) + "3"
        cardNumberEditText.setText(notQuiteValid)
        assertTrue(cardNumberEditText.shouldShowError)

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertFalse(cardNumberEditText.shouldShowError)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + DINERS_CLUB_16_WITH_SPACES.last())
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingAmEx_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        val notQuiteValid = AMEX_WITH_SPACES.substring(0, 16) + "3"
        cardNumberEditText.setText(notQuiteValid)
        assertTrue(cardNumberEditText.shouldShowError)

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertFalse(cardNumberEditText.shouldShowError)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "5")
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun setCardBrandChangeListener_callsSetCardBrand() {
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun addVisaPrefix_callsBrandListener() {
        // We reset because just attaching the listener calls the method once.
        lastBrandChangeCallbackInvocation = null
        // There is only one Visa Prefix.
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + CardBrand.Visa.prefixes[0])
        assertEquals(CardBrand.Visa, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun addAmExPrefix_callsBrandListener() {
        CardBrand.AmericanExpress.prefixes.forEach(
            verifyCardBrandPrefix(CardBrand.AmericanExpress)
        )
    }

    @Test
    fun addDinersClubPrefix_callsBrandListener() {
        CardBrand.DinersClub.prefixes.forEach(verifyCardBrandPrefix(CardBrand.DinersClub))
    }

    @Test
    fun addDiscoverPrefix_callsBrandListener() {
        CardBrand.Discover.prefixes.forEach(verifyCardBrandPrefix(CardBrand.Discover))
    }

    @Test
    fun addMasterCardPrefix_callsBrandListener() {
        CardBrand.MasterCard.prefixes.forEach(verifyCardBrandPrefix(CardBrand.MasterCard))
    }

    @Test
    fun addJCBPrefix_callsBrandListener() {
        CardBrand.JCB.prefixes.forEach(verifyCardBrandPrefix(CardBrand.JCB))
    }

    @Test
    fun enterCompleteNumberInParts_onlyCallsBrandListenerOnce() {
        lastBrandChangeCallbackInvocation = null
        val prefix = AMEX_WITH_SPACES.substring(0, 2)
        val suffix = AMEX_WITH_SPACES.substring(2)
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefix)
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + suffix)
        assertEquals(CardBrand.AmericanExpress, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterBrandPrefix_thenDelete_callsUpdateWithUnknown() {
        lastBrandChangeCallbackInvocation = null
        val dinersPrefix = CardBrand.DinersClub.prefixes[0]
        // All the Diners Club prefixes are longer than one character, but I specifically want
        // to test that a nonempty string still triggers this action, so this test will fail if
        // the Diners Club prefixes are ever changed.
        assertTrue(dinersPrefix.length > 1)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + dinersPrefix)
        assertEquals(CardBrand.DinersClub, lastBrandChangeCallbackInvocation)

        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterBrandPrefix_thenClearAllText_callsUpdateWithUnknown() {
        lastBrandChangeCallbackInvocation = null
        val prefixVisa = CardBrand.Visa.prefixes[0]
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefixVisa)
        assertEquals(CardBrand.Visa, lastBrandChangeCallbackInvocation)

        // Just adding some other text. Not enough to invalidate the card or complete it.
        lastBrandChangeCallbackInvocation = null
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "123")
        assertNull(lastBrandChangeCallbackInvocation)

        // This simulates the user selecting all text and deleting it.
        cardNumberEditText.setText("")

        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun cardNumber_withSpaces_returnsCardNumberWithoutSpaces() {
        cardNumberEditText.setText(VISA_WITH_SPACES)
        assertEquals(VISA_NO_SPACES, cardNumberEditText.cardNumber)

        cardNumberEditText.setText("")
        cardNumberEditText.setText(AMEX_WITH_SPACES)
        assertEquals(AMEX_NO_SPACES, cardNumberEditText.cardNumber)

        cardNumberEditText.setText("")
        cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)
        assertEquals(DINERS_CLUB_14_NO_SPACES, cardNumberEditText.cardNumber)

        cardNumberEditText.setText("")
        cardNumberEditText.setText(DINERS_CLUB_16_WITH_SPACES)
        assertEquals(DINERS_CLUB_16_NO_SPACES, cardNumberEditText.cardNumber)
    }

    @Test
    fun getCardNumber_whenIncompleteCard_returnsNull() {
        cardNumberEditText.setText(
            DINERS_CLUB_14_WITH_SPACES
                .substring(0, DINERS_CLUB_14_WITH_SPACES.length - 2))
        assertNull(cardNumberEditText.cardNumber)
    }

    @Test
    fun getCardNumber_whenInvalidCardNumber_returnsNull() {
        var almostVisa = VISA_WITH_SPACES.substring(0, VISA_WITH_SPACES.length - 1)
        almostVisa += "3" // creates the 4242 4242 4242 4243 bad number
        cardNumberEditText.setText(almostVisa)
        assertNull(cardNumberEditText.cardNumber)
    }

    @Test
    fun getCardNumber_whenValidNumberIsChangedToInvalid_returnsNull() {
        cardNumberEditText.setText(AMEX_WITH_SPACES)
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)

        assertNull(cardNumberEditText.cardNumber)
    }

    @Test
    fun testUpdateCardBrandFromNumber() {
        cardNumberEditText.updateCardBrandFromNumber(DINERS_CLUB_14_NO_SPACES)
        assertEquals(CardBrand.DinersClub, lastBrandChangeCallbackInvocation)
        cardNumberEditText.updateCardBrandFromNumber(AMEX_NO_SPACES)
        assertEquals(CardBrand.AmericanExpress, lastBrandChangeCallbackInvocation)
    }

    private fun verifyCardBrandPrefix(cardBrand: CardBrand): (String) -> Unit {
        return { prefix ->
            // Reset inside the loop so we don't count each prefix
            lastBrandChangeCallbackInvocation = null
            cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefix)
            assertEquals(cardBrand, lastBrandChangeCallbackInvocation)
            cardNumberEditText.setText("")
        }
    }
}
