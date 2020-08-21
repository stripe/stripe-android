package com.stripe.android.view

import android.content.Context
import android.os.Looper
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_WITH_SPACES
import com.stripe.android.CardNumberFixtures.JCB_NO_SPACES
import com.stripe.android.CardNumberFixtures.JCB_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.AccountRangeFixtures
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.LegacyCardAccountRangeRepository
import com.stripe.android.cards.LocalCardAccountRangeSource
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardMetadata
import com.stripe.android.testharness.ViewTestUtils
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

/**
 * Test class for [CardNumberEditText].
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
internal class CardNumberEditTextTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private var completionCallbackInvocations = 0
    private val completionCallback: () -> Unit = { completionCallbackInvocations++ }

    private var lastBrandChangeCallbackInvocation: CardBrand? = null
    private val brandChangeCallback: (CardBrand) -> Unit = {
        lastBrandChangeCallbackInvocation = it
    }

    private val cardAccountRangeRepository = LegacyCardAccountRangeRepository(
        LocalCardAccountRangeSource()
    )

    private val cardNumberEditText = CardNumberEditText(
        context,
        workDispatcher = testDispatcher,
        cardAccountRangeRepository = cardAccountRangeRepository
    )

    @BeforeTest
    fun setup() {
        cardNumberEditText.setText("")
        cardNumberEditText.completionCallback = completionCallback
        cardNumberEditText.brandChangeCallback = brandChangeCallback
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateSelectionIndex_whenVisa_increasesIndexWhenGoingPastTheSpaces() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.VISA
        )

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
    fun updateSelectionIndex_whenAmEx_increasesIndexWhenGoingPastTheSpaces() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.AMERICANEXPRESS
        )

        assertThat(
            cardNumberEditText.updateSelectionIndex(6, 4, 1)
        ).isEqualTo(6)
        assertThat(
            cardNumberEditText.updateSelectionIndex(13, 11, 1)
        ).isEqualTo(13)
    }

    @Test
    fun updateSelectionIndex_whenDinersClub16_decreasesIndexWhenDeletingPastTheSpaces() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.DINERSCLUB16
        )

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
    fun updateSelectionIndex_whenDeletingNotOnGaps_doesNotDecreaseIndex() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.DINERSCLUB14
        )

        assertThat(
            cardNumberEditText.updateSelectionIndex(12, 7, 0)
        ).isEqualTo(7)
    }

    @Test
    fun updateSelectionIndex_whenAmEx_decreasesIndexWhenDeletingPastTheSpaces() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.AMERICANEXPRESS
        )

        assertThat(
            cardNumberEditText.updateSelectionIndex(10, 5, 0)
        ).isEqualTo(4)
        assertThat(
            cardNumberEditText.updateSelectionIndex(13, 12, 0)
        ).isEqualTo(11)
    }

    @Test
    fun updateSelectionIndex_whenSelectionInTheMiddle_increasesIndexOverASpace() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.VISA
        )

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
    fun `valid Amex should change isCardNumberValid to true and invoke completion callback`() {
        // type Amex BIN
        updateCardNumberAndIdle(CardNumberFixtures.AMEX_BIN)
        // type rest of card number
        cardNumberEditText.append(AMEX_NO_SPACES.drop(6))
        idle()

        assertThat(cardNumberEditText.isCardNumberValid)
            .isTrue()
        assertThat(completionCallbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun setText_whenTextChangesFromValidToInvalid_changesCardValidState() {
        updateCardNumberAndIdle(VISA_WITH_SPACES)
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
        updateCardNumberAndIdle(
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

        updateCardNumberAndIdle("123")
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenValidCard_doesNotSetErrorValue() {
        updateCardNumberAndIdle(withoutLastCharacter(VISA_WITH_SPACES))
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        // We now have the valid 4242 Visa
        cardNumberEditText.append("2")
        idle()

        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenInvalidCard_setsErrorValue() {
        updateCardNumberAndIdle(withoutLastCharacter(VISA_WITH_SPACES))

        // This makes the number officially invalid
        cardNumberEditText.append("3")
        idle()

        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()
    }

    @Test
    fun finishTypingInvalidCardNumber_whenFollowedByDelete_setsErrorBackToFalse() {
        updateCardNumberAndIdle(
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
        // type DinersClub BIN
        updateCardNumberAndIdle(CardNumberFixtures.DINERS_CLUB_14_BIN)
        // type rest of card number
        cardNumberEditText.append(
            withoutLastCharacter(DINERS_CLUB_14_WITH_SPACES.drop(6)) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        idle()
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append(DINERS_CLUB_14_WITH_SPACES.last().toString())
        idle()

        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingDinersClub16_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        updateCardNumberAndIdle(
            withoutLastCharacter(DINERS_CLUB_16_WITH_SPACES) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append(DINERS_CLUB_16_WITH_SPACES.last().toString())
        idle()

        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingAmEx_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        // type Amex BIN
        updateCardNumberAndIdle(CardNumberFixtures.AMEX_BIN)
        // type rest of card number
        cardNumberEditText.append(
            withoutLastCharacter(AMEX_NO_SPACES.drop(6)) + "3"
        )
        idle()
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        idle()
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append("5")
        idle()

        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun setCardBrandChangeListener_callsSetCardBrand() {
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterVisaBin_callsBrandListener() {
        updateCardNumberAndIdle(CardNumberFixtures.VISA_BIN)
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
        idle()
        cardNumberEditText.append(AMEX_WITH_SPACES.drop(2))
        idle()
        assertEquals(CardBrand.AmericanExpress, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterBrandBin_thenDelete_callsUpdateWithUnknown() {
        updateCardNumberAndIdle(CardNumberFixtures.DINERS_CLUB_14_BIN)
        assertEquals(CardBrand.DinersClub, lastBrandChangeCallbackInvocation)

        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        idle()
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterBrandBin_thenClearAllText_callsUpdateWithUnknown() {
        updateCardNumberAndIdle(CardNumberFixtures.VISA_BIN)
        assertEquals(CardBrand.Visa, lastBrandChangeCallbackInvocation)

        // Just adding some other text. Not enough to invalidate the card or complete it.
        lastBrandChangeCallbackInvocation = null
        cardNumberEditText.append("123")
        idle()

        assertNull(lastBrandChangeCallbackInvocation)

        // This simulates the user selecting all text and deleting it.
        updateCardNumberAndIdle("")

        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun cardNumber_withSpaces_returnsCardNumberWithoutSpaces() {
        updateCardNumberAndIdle(VISA_WITH_SPACES)
        assertThat(cardNumberEditText.cardNumber)
            .isEqualTo(VISA_NO_SPACES)

        updateCardNumberAndIdle("")
        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        assertThat(cardNumberEditText.cardNumber)
            .isEqualTo(AMEX_NO_SPACES)

        updateCardNumberAndIdle("")
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)
        assertThat(cardNumberEditText.cardNumber)
            .isEqualTo(DINERS_CLUB_14_NO_SPACES)

        updateCardNumberAndIdle("")
        updateCardNumberAndIdle(DINERS_CLUB_16_WITH_SPACES)
        assertThat(cardNumberEditText.cardNumber)
            .isEqualTo(DINERS_CLUB_16_NO_SPACES)
    }

    @Test
    fun getCardNumber_whenIncompleteCard_returnsNull() {
        updateCardNumberAndIdle(
            DINERS_CLUB_14_WITH_SPACES.take(DINERS_CLUB_14_WITH_SPACES.length - 2)
        )
        assertThat(cardNumberEditText.cardNumber)
            .isNull()
    }

    @Test
    fun getCardNumber_whenInvalidCardNumber_returnsNull() {
        updateCardNumberAndIdle(
            withoutLastCharacter(VISA_WITH_SPACES) + "3" // creates the 4242 4242 4242 4243 bad number
        )
        assertThat(cardNumberEditText.cardNumber)
            .isNull()
    }

    @Test
    fun getCardNumber_whenValidNumberIsChangedToInvalid_returnsNull() {
        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)

        assertThat(cardNumberEditText.cardNumber)
            .isNull()
    }

    @Test
    fun `pasting a full number, fully deleting it via delete key, then pasting a new full number should format the new number`() {
        // paste a full number
        updateCardNumberAndIdle(VISA_NO_SPACES)

        // fully delete it with delete key
        repeat(cardNumberEditText.fieldText.length) {
            ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        }
        assertThat(cardNumberEditText.fieldText)
            .isEmpty()

        // paste a new number
        updateCardNumberAndIdle(JCB_NO_SPACES)

        assertThat(cardNumberEditText.fieldText)
            .isEqualTo(JCB_WITH_SPACES)
    }

    @Test
    fun `updateCardBrand() should update cardBrand value`() {
        cardNumberEditText.updateCardBrand(BinFixtures.DINERSCLUB14)
        idle()
        assertEquals(CardBrand.DinersClub, lastBrandChangeCallbackInvocation)

        cardNumberEditText.updateCardBrand(BinFixtures.AMEX)
        idle()
        assertEquals(CardBrand.AmericanExpress, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun `updateCardBrand() with null bin should set cardBrand to Unknown`() {
        cardNumberEditText.updateCardBrand(null)
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun `onDetachedFromWindow() should cancel accountRangeRepositoryJob`() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        activityScenarioFactory.createAddPaymentMethodActivity()
            .use { activityScenario ->
                activityScenario.onActivity { activity ->
                    val cardNumberEditText = CardNumberEditText(
                        activity,
                        workDispatcher = testDispatcher,
                        cardAccountRangeRepository = DelayedCardAccountRangeRepository()
                    )

                    val root = activity.findViewById<ViewGroup>(R.id.add_payment_method_card).also {
                        it.removeAllViews()
                        it.addView(cardNumberEditText)
                    }

                    cardNumberEditText.setText(CardNumberFixtures.VISA_NO_SPACES)
                    assertThat(cardNumberEditText.accountRangeRepositoryJob)
                        .isNotNull()

                    root.removeView(cardNumberEditText)
                    assertThat(cardNumberEditText.accountRangeRepositoryJob)
                        .isNull()
                }
            }
    }

    @Test
    fun `isProcessingCallback should be invoked with 'true' when fetching account range and 'false' when fetch is completed`() {
        var isProcessing = false
        cardNumberEditText.isProcessingCallback = {
            isProcessing = it
        }

        // not processing at rest
        assertThat(isProcessing)
            .isFalse()

        // start processing once a BIN is typed in
        cardNumberEditText.setText(BinFixtures.VISA.value)
        assertThat(isProcessing)
            .isTrue()
        idle()

        // account range data has been retrieved
        assertThat(isProcessing)
            .isFalse()
    }

    private fun verifyCardBrandBin(
        cardBrand: CardBrand,
        bin: String
    ) {
        // Reset inside the loop so we don't count each prefix
        lastBrandChangeCallbackInvocation = null
        updateCardNumberAndIdle(bin)
        assertEquals(cardBrand, lastBrandChangeCallbackInvocation)
        updateCardNumberAndIdle("")
    }

    private fun updateCardNumberAndIdle(cardNumber: String) {
        cardNumberEditText.setText(cardNumber)
        idle()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private class DelayedCardAccountRangeRepository : CardAccountRangeRepository {
        override suspend fun getAccountRange(cardNumber: String): CardMetadata.AccountRange? {
            delay(TimeUnit.SECONDS.toMillis(10))
            return null
        }
    }

    private companion object {
        private fun withoutLastCharacter(s: String) = s.take(s.length - 1)
    }
}
